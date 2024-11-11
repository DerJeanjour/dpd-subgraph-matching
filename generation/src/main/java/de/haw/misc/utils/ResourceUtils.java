package de.haw.misc.utils;

import java.net.URL;

public class ResourceUtils {

    public static URL getResourceFileUrl( final String resourcePath ) {
        return FileUtils.class.getClassLoader().getResource( resourcePath );
    }

}
