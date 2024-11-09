package frontend;

import backend.bot.evaluator.PiecePointChessEvaluator;
import backend.core.exception.NotationParsingException;
import backend.core.model.Move;
import backend.core.model.MoveHistory;
import backend.core.model.Piece;
import backend.core.model.Validation;
import backend.core.notation.AlgebraicNotation;
import backend.core.notation.ChessNotation;
import backend.core.notation.FenNotation;
import backend.core.values.ActionType;
import backend.core.values.PieceType;
import backend.core.values.PlayerType;
import backend.core.values.TeamColor;
import backend.game.Game;
import backend.game.GameListener;
import backend.game.Player;
import math.Color;
import math.Vector2I;
import misc.FpsTracker;
import misc.Log;
import util.IOUtil;
import util.MathUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class GameView implements GameListener {

    private static final Map<ActionType, Color> actionColors = Map.of(
            ActionType.MOVE, Color.GREEN,
            ActionType.CAPTURE, Color.RED,
            ActionType.CAPTURE_AU_PASSANT, Color.RED,
            ActionType.CASTLE_QUEEN, Color.GREEN,
            ActionType.CASTLE_KING, Color.GREEN,
            ActionType.CHECK, Color.RED
    );

    private final FpsTracker fps;

    private final Game game;

    private Player whitePlayer;

    private Player blackPlayer;

    private String notation;

    private PieceType promotionMode;

    private final int boardSize;

    private final int windowW;

    private final int windowH;

    private final int posSize;

    private final int xOff;

    private final int yOff;

    private SpriteProvider sprites;

    private ChessNotation notationProcessor;

    private JFrame frame;

    private JTextArea moveInfo;

    private JLabel fpsInfo;

    private JTextField gameStateInfo;

    private Vector2I selectedPos;

    private boolean showMovePreview;

    private boolean showAttacked;

    private boolean showPined;

    private boolean onDrag;

    private Map<Vector2I, Validation> validation;

    public GameView( Game game, int boardSize, int windowW, int windowH ) {

        this.fps = new FpsTracker( 1000L );
        this.fps.start();

        this.game = game;
        this.game.addListener( this );
        this.whitePlayer = new Player( TeamColor.WHITE, PlayerType.HUMAN );
        this.blackPlayer = new Player( TeamColor.BLACK, PlayerType.HUMAN );

        this.notation = "";
        this.boardSize = boardSize;
        this.promotionMode = PieceType.QUEEN;

        this.windowW = windowW;
        this.windowH = windowH;
        this.posSize = boardSize / game.getBoardSize();
        this.xOff = ( windowW - boardSize ) / 2;
        this.yOff = ( windowH - boardSize ) / 2;

        this.selectedPos = null;
        this.onDrag = false;
        this.showMovePreview = false;
        this.showAttacked = false;
        this.showPined = false;
        this.validation = new HashMap<>();
        this.sprites = new SpriteProvider();
        this.sprites.reload( this.posSize );
        this.notationProcessor = new AlgebraicNotation();
        setupFrame();

        this.game.emitEvent();
    }

    @Override
    public void gameUpdated( Game game ) {
        this.notation = notationProcessor.write( game );
        final Player onMove = this.whitePlayer.isOnMove( game )
                ? this.whitePlayer
                : this.blackPlayer;
        new SwingWorker<>() {
            @Override
            protected Game doInBackground() {
                onMove.makeMove( game );
                return game;
            }
        }.execute();
    }

    private void setupFrame() {

        // overall window frame
        frame = new JFrame( "Chess" );
        frame.setResizable( false );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setLocationRelativeTo( null );
        frame.setLayout( new BorderLayout() );

        // panel that holds the game
        JPanel gamePanel = new JPanel();
        gamePanel.setPreferredSize( new Dimension( this.windowW, this.windowH ) );
        frame.add( gamePanel, BorderLayout.CENTER );

        // the game board
        Draw drawGame = new Draw();
        drawGame.setBounds( 0, 0, this.windowW, this.windowH );
        drawGame.setPreferredSize( new Dimension( this.windowW, this.windowH ) );
        drawGame.setVisible( true );
        gamePanel.add( drawGame );

        // mouse and key listener
        gamePanel.addMouseListener( new MouseAdapter() {

            @Override
            public void mousePressed( MouseEvent e ) {
                selectedPos = pixelToPosition( e.getX(), e.getY() );
                if ( selectedPos != null && !game.isOutOfBounds( selectedPos ) ) {
                    if ( showMovePreview ) {
                        game.validate( selectedPos ).forEach( v -> validation.put( v.getMove().getTo(), v ) );
                    }
                    onDrag = true;
                }
            }

            @Override
            public void mouseReleased( MouseEvent e ) {
                Vector2I pos = pixelToPosition( e.getX(), e.getY() );
                if ( selectedPos != null && !game.isOutOfBounds( pos ) ) {
                    Player playerOnMove = game.isOnMove( TeamColor.WHITE ) ? whitePlayer : blackPlayer;
                    if ( playerOnMove.isHuman() ) {
                        Move move = new Move( selectedPos, pos, promotionMode );
                        game.makeMove( move );
                    }
                }
                selectedPos = null;
                validation.clear();
                onDrag = false;
            }
        } );

        gamePanel.addKeyListener( new KeyAdapter() {
            @Override
            public void keyPressed( KeyEvent e ) {
                if ( e.getKeyCode() == KeyEvent.VK_SPACE ) {
                    Log.info( "Pressed Space" );
                }
                if ( e.getKeyCode() == KeyEvent.VK_ENTER ) {
                    Log.info( "Enter" );
                }
            }
        } );

        // info panel that holds moves, buttons, settings ...
        int infoSizeWidth = 300;
        JPanel infoPanel = new JPanel();
        infoPanel.setPreferredSize( new Dimension( infoSizeWidth, this.windowH ) );
        frame.add( infoPanel, BorderLayout.EAST );

        this.moveInfo = new JTextArea();
        moveInfo.setPreferredSize( new Dimension( infoSizeWidth - 10, this.windowH / 2 ) );
        moveInfo.setEditable( true );
        moveInfo.addKeyListener( new KeyListener() {

            @Override
            public void keyTyped( KeyEvent e ) {

            }

            @Override
            public void keyPressed( KeyEvent e ) {

                switch ( e.getKeyCode() ) {
                    case KeyEvent.VK_LEFT: // FIXME
                        moveInfo.moveCaretPosition( MathUtil.clamp( 0, moveInfo.getText().length(), moveInfo.getCaretPosition() - 1 ) );
                        break;
                    case KeyEvent.VK_RIGHT: // FIXME
                        moveInfo.moveCaretPosition( MathUtil.clamp( 0, moveInfo.getText().length(), moveInfo.getCaretPosition() + 1 ) );
                        break;
                    case KeyEvent.VK_DELETE:
                    case KeyEvent.VK_BACK_SPACE:
                        if ( !notation.isEmpty() ) {
                            notation = notation.substring( 0, notation.length() - 1 );
                        }
                        break;
                    default:
                        char pressed = e.getKeyChar();
                        if ( Character.isDefined( pressed ) ) {
                            notation += pressed;
                        }
                        break;
                }

            }

            @Override
            public void keyReleased( KeyEvent e ) {

            }

        } );
        moveInfo.setLineWrap( true );
        JScrollPane moveInfoScroll = new JScrollPane( moveInfo );
        infoPanel.add( moveInfoScroll );

        JButton notationSwitchButton = new JButton( "Switch notation" );
        notationSwitchButton.addActionListener( e -> {
            this.notationProcessor = this.notationProcessor instanceof AlgebraicNotation
                    ? new FenNotation()
                    : new AlgebraicNotation();
            this.notation = notationProcessor.write( game );
        } );
        infoPanel.add( notationSwitchButton );

        JButton copyHistoryButton = new JButton( "Copy" );
        copyHistoryButton.addActionListener( e -> IOUtil.copyToClipboard( moveInfo.getText() ) );
        infoPanel.add( copyHistoryButton );

        JButton parseButton = new JButton( "Parse" );
        parseButton.addActionListener( a -> {
            try {
                game.setGame( this.notation, this.notationProcessor );
            } catch ( NotationParsingException e ) {
                this.notation = notationProcessor.write( game );
            }

        } );
        infoPanel.add( parseButton );

        JButton backButton = new JButton( "Go Back" );
        backButton.addActionListener( e -> {
            this.game.undoLastMove();
        } );
        infoPanel.add( backButton );

        JButton resetButton = new JButton( "Reset" );
        resetButton.addActionListener( e -> {
            this.game.reset();
            this.validation.clear();
            this.onDrag = false;
        } );
        infoPanel.add( resetButton );

        JComboBox<PieceType> promotionModeSelect = new JComboBox<>( new PieceType[]{ PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT } );
        promotionModeSelect.setSelectedItem( this.promotionMode );
        promotionModeSelect.addActionListener( e -> promotionMode = ( PieceType ) promotionModeSelect.getSelectedItem() );
        infoPanel.add( promotionModeSelect );

        JCheckBox movePreviewButton = new JCheckBox( "Move Preview" );
        movePreviewButton.addActionListener( e -> showMovePreview = movePreviewButton.isSelected() );
        infoPanel.add( movePreviewButton );

        JCheckBox showAttackedButton = new JCheckBox( "Show attacked" );
        showAttackedButton.addActionListener( e -> showAttacked = showAttackedButton.isSelected() );
        infoPanel.add( showAttackedButton );

        JCheckBox showPinedButton = new JCheckBox( "Show pined" );
        showPinedButton.addActionListener( e -> showPined = showPinedButton.isSelected() );
        infoPanel.add( showPinedButton );

        JPanel whitePlayerSelectPanel = new JPanel();
        whitePlayerSelectPanel.setLayout( new BoxLayout( whitePlayerSelectPanel, BoxLayout.LINE_AXIS ) );
        whitePlayerSelectPanel.add( new JLabel( "White" ) );
        whitePlayerSelectPanel.add( Box.createHorizontalGlue() );
        JComboBox<PlayerType> whitePlayerSelect = new JComboBox<>( PlayerType.values() );
        whitePlayerSelect.setSelectedItem( this.whitePlayer.getType() );
        whitePlayerSelect.addActionListener( e -> {
            this.whitePlayer = new Player( TeamColor.WHITE, ( PlayerType ) whitePlayerSelect.getSelectedItem() );
            this.gameUpdated( this.game );
        } );
        whitePlayerSelectPanel.add( whitePlayerSelect );
        infoPanel.add( whitePlayerSelectPanel );

        JPanel blackPlayerSelectPanel = new JPanel();
        blackPlayerSelectPanel.setLayout( new BoxLayout( blackPlayerSelectPanel, BoxLayout.LINE_AXIS ) );
        blackPlayerSelectPanel.add( new JLabel( "Black" ) );
        JComboBox<PlayerType> blackPlayerSelect = new JComboBox<>( PlayerType.values() );
        blackPlayerSelect.setSelectedItem( this.blackPlayer.getType() );
        blackPlayerSelect.addActionListener( e -> {
            this.blackPlayer = new Player( TeamColor.BLACK, ( PlayerType ) blackPlayerSelect.getSelectedItem() );
            this.gameUpdated( this.game );
        } );
        blackPlayerSelectPanel.add( blackPlayerSelect );
        infoPanel.add( blackPlayerSelectPanel );

        this.gameStateInfo = new JTextField();
        this.gameStateInfo.setEditable( false );
        this.gameStateInfo.setPreferredSize( new Dimension( infoSizeWidth - 10, 30 ) );
        infoPanel.add( this.gameStateInfo );

        this.fpsInfo = new JLabel( fps.getPrintableFps() + "FPS" );
        infoPanel.add( this.fpsInfo );

        frame.pack();
        frame.requestFocus();
        frame.setVisible( true );

    }

    public Vector2I pixelToPosition( int x, int y ) {
        int posX = ( x - this.xOff ) / this.posSize;
        int posY = ( y - ( this.yOff + 7 ) ) / this.posSize;
        return new Vector2I( posX, this.game.getBoardSize() - 1 - posY );
    }

    public Vector2I positionToPixel( int x, int y ) {
        int pixelX = this.xOff + ( x * this.posSize );
        int pixelY = this.yOff + ( y * this.posSize );
        return new Vector2I( pixelX, pixelY );
    }

    public class Draw extends JLabel {

        @Override
        protected void paintComponent( Graphics g ) {

            super.paintComponent( g );
            Graphics2D g2d = ( Graphics2D ) g;
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );

            // Background
            g.setColor( java.awt.Color.lightGray );
            g.fillRect( 0, 0, windowW, windowH );

            // Draw Positions
            for ( int i = 0; i < game.getBoardSize(); i++ ) {
                for ( int j = 0; j < game.getBoardSize(); j++ ) {

                    Vector2I p = positionToPixel( i, j );
                    Vector2I pos = new Vector2I( i, game.getBoardSize() - 1 - j );

                    Color posColor = ( i + j ) % 2 != 0 ? Color.DARK_GREY : Color.LIGHT_GREY;

                    // draw temp from pos
                    if ( onDrag && selectedPos != null && pos.equals( selectedPos ) ) {
                        posColor = posColor.blend( new Color( Color.BLUE, 0.2f ) );
                    }

                    if ( !onDrag && game.getLastMove() != null ) {
                        MoveHistory lastMove = game.getLastMove();
                        if ( pos.equals( lastMove.getMove().getFrom() ) ) {
                            posColor = posColor.blend( new Color( Color.BLUE, 0.2f ) );
                        }
                        if ( pos.equals( lastMove.getMove().getTo() ) ) {
                            posColor = posColor.blend( new Color( Color.GREEN, 0.2f ) );
                        }
                    }

                    // draw move preview
                    if ( showMovePreview && game.isLegal( validation, pos ) ) {

                        Color actionColor = new Color( Color.BLACK, 0f );

                        if ( game.hasAction( validation, pos, ActionType.MOVE ) ) {
                            actionColor = new Color( actionColors.get( ActionType.MOVE ), 0.2f );
                        }

                        if ( game.hasAction( validation, pos, ActionType.CAPTURE ) ) {
                            actionColor = new Color( actionColors.get( ActionType.CAPTURE ), 0.2f );
                        }

                        if ( game.hasAction( validation, pos, ActionType.CAPTURE_AU_PASSANT ) ) {
                            actionColor = new Color( actionColors.get( ActionType.CAPTURE_AU_PASSANT ), 0.2f );
                        }

                        if ( game.hasAction( validation, pos, ActionType.CHECK ) ) {
                            actionColor = new Color( actionColors.get( ActionType.CHECK ), 0.2f );
                        }

                        posColor = posColor.blend( actionColor );
                    }

                    if ( !onDrag && showAttacked && game.isAttacked( pos ) ) {
                        posColor = posColor.blend( new Color( Color.RED, 0.2f ) );
                    }

                    if ( !onDrag && showPined && game.isPined( pos ) ) {
                        posColor = posColor.blend( new Color( Color.PINK, 0.2f ) );
                    }

                    g.setColor( new java.awt.Color( posColor.getInt() ) );
                    g.fillRect( p.x, p.y, posSize, posSize );
                }
            }

            // Draw Cell Description
            g.setColor( java.awt.Color.black );
            for ( int j = 0; j < game.getBoardSize(); j++ ) {
                for ( int i = 0; i < game.getBoardSize(); i++ ) {
                    Vector2I p = positionToPixel( i, j );
                    Vector2I boardPos = new Vector2I( i, game.getBoardSize() - 1 - j );
                    if ( i == 0 ) {
                        g.drawString( AlgebraicNotation.getRowCode( boardPos ), p.x - 20, p.y + 15 );
                    }
                    if ( j == game.getBoardSize() - 1 ) {
                        g.drawString( AlgebraicNotation.getColCode( boardPos ), p.x + posSize - 15, p.y + posSize + 15 );
                    }
                }
            }

            // Draw Pieces
            for ( int j = 0; j < game.getBoardSize(); j++ ) {
                for ( int i = 0; i < game.getBoardSize(); i++ ) {

                    Vector2I p = positionToPixel( i, j );
                    Vector2I boardPos = new Vector2I( i, game.getBoardSize() - 1 - j );

                    if ( !onDrag || !selectedPos.equals( boardPos ) ) {
                        drawPiece( g, p, boardPos );
                    }
                }
            }

            // Draw Dragged Piece
            if ( onDrag && selectedPos != null && this.getMousePosition() != null ) {
                Point p = this.getMousePosition();
                int x = p.x - ( posSize / 2 );
                int y = p.y - ( posSize / 2 );
                drawPiece( g, new Vector2I( x, y ), selectedPos );
            }

            // Draw Grid Outlines
            g.setColor( java.awt.Color.black );
            Vector2I p = positionToPixel( 0, 0 );
            g.drawRect( p.x, p.y, boardSize, boardSize );

            // Set info texts
            moveInfo.setText( notation );
            gameStateInfo.setText( game.getState().name() );

            fps.update();
            fpsInfo.setText( fps.getPrintableFps() + "FPS" );

            repaint();

        }

    }

    private void drawPiece( Graphics g, Vector2I p, Vector2I pos ) {
        Piece piece = game.getPiece( pos );
        if ( piece != null && piece.isAlive() ) {
            g.drawImage( this.sprites.getPieceSprite( piece.getType(), piece.getTeam() ), p.x, p.y, null );
        }
    }

}
