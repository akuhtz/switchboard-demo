package org.bidib.switchboard.util;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for loading and caching SVG documents using jsvg.
 */
public class SvgIconLoader {

    private static final SVGLoader LOADER = new SVGLoader();
    private static final Map<String, SVGDocument> CACHE = new ConcurrentHashMap<>();

    private SvgIconLoader() {
    }

    /**
     * Loads an SVG document from a classpath resource path.
     * Results are cached for repeated lookups.
     *
     * @param resourcePath classpath path, e.g. "/icons/straight.svg"
     * @return the parsed SVGDocument, or null if loading failed
     */
    public static SVGDocument load(String resourcePath) {
        return CACHE.computeIfAbsent(resourcePath, path -> {
            URL url = SvgIconLoader.class.getResource(path);
            if (url == null) {
                return null;
            }
            return LOADER.load(url);
        });
    }
}
