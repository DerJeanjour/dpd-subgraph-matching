copy( JSON.stringify( Object.fromEntries( Object.entries( localStorage ).map( ( [ k, v ] ) => [
    k, ( () => {
        try {
            return JSON.parse( v );
        } catch {
            return v;
        }
    } )()
] ) ), null, 2 ) );