package backend.core.exception;

import misc.Log;

public class NotationParsingException extends RuntimeException {

    public NotationParsingException( String reasonPattern, Object... arguments ) {
        Log.error( reasonPattern, arguments );
    }

}
