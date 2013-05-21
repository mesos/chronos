package com.airbnb.dropwizard.assets;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.cache.*;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.yammer.dropwizard.assets.ResourceURL;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Buffer;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Servlet responsible for serving assets to the caller.  This is basically completely stolen from
 * {@link com.yammer.dropwizard.assets.AssetServlet} with the exception of allowing for override options.
 *
 * @see com.yammer.dropwizard.assets.AssetServlet
 */
class AssetServlet extends HttpServlet {
    private static final long serialVersionUID = 6393345594784987908L;
    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.HTML_UTF_8;
    private static final String DEFAULT_INDEX_FILE = "index.html";

    private final transient LoadingCache<String, Asset> cache;
    private final transient MimeTypes mimeTypes;

    private Charset defaultCharset = Charsets.UTF_8;

    /**
     * Creates a new {@code AssetServlet} that serves static assets loaded from {@code resourceURL} (typically a file:
     * or jar: URL). The assets are served at URIs rooted at {@code uriPath}. For example, given a {@code resourceURL}
     * of {@code "file:/data/assets"} and a {@code uriPath} of {@code "/js"}, an {@code AssetServlet} would serve the
     * contents of {@code /data/assets/example.js} in response to a request for {@code /js/example.js}. If a directory
     * is requested and {@code indexFile} is defined, then {@code AssetServlet} will attempt to serve a file with that
     * name in that directory. If a directory is requested and {@code indexFile} is null, it will serve a 404.
     *
     * @param resourcePath the base URL from which assets are loaded
     * @param spec         specification for the underlying cache
     * @param uriPath      the URI path fragment in which all requests are rooted
     * @param indexFile    the filename to use when directories are requested, or null to serve no indexes
     * @param overrides    the path overrides
     * @see com.google.common.cache.CacheBuilderSpec
     */
    public AssetServlet(String resourcePath, CacheBuilderSpec spec, String uriPath, String indexFile,
            Iterable<Map.Entry<String, String>> overrides) {
        AssetLoader loader = new AssetLoader(resourcePath, uriPath, indexFile, overrides);
        this.cache = CacheBuilder.from(spec).weigher(new AssetSizeWeigher()).build(loader);
        this.mimeTypes = new MimeTypes();
        this.mimeTypes.addMimeMapping("js", "application/javascript");
    }

    /**
     * Creates a new {@code AssetServlet}. This is provided for backwards-compatibility; see
     * {@link AssetServlet( java.net.URL , CacheBuilderSpec, String, String)} for details.
     *
     * @param resourcePath the base URL from which assets are loaded
     * @param spec         specification for the underlying cache
     * @param uriPath      the URI path fragment in which all requests are rooted
     */
    public AssetServlet(String resourcePath, CacheBuilderSpec spec, String uriPath,
            Iterable<Map.Entry<String, String>> overrides) {
        this(resourcePath, spec, uriPath, DEFAULT_INDEX_FILE, overrides);
    }

    public void setDefaultCharset(Charset defaultCharset) {
        this.defaultCharset = defaultCharset;
    }

    public Charset getDefaultCharset() {
        return this.defaultCharset;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            final StringBuilder builder = new StringBuilder(req.getServletPath());
            if (req.getPathInfo() != null) {
                builder.append(req.getPathInfo());
            }
            Asset asset = cache.getUnchecked(builder.toString());
            if (asset == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Check the etag...
            if (asset.getETag().equals(req.getHeader(HttpHeaders.IF_NONE_MATCH))) {
                resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }

            // Check the last modified time...
            if (asset.getLastModifiedTime() <= req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE)) {
                resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }

            resp.setDateHeader(HttpHeaders.LAST_MODIFIED, asset.getLastModifiedTime());
            resp.setHeader(HttpHeaders.ETAG, asset.getETag());

            MediaType mediaType = DEFAULT_MEDIA_TYPE;

            Buffer mimeType = mimeTypes.getMimeByExtension(req.getRequestURI());

            if (mimeType != null) {
                try {
                    mediaType = MediaType.parse(mimeType.toString());
                    if (defaultCharset != null && !mediaType.charset().isPresent()) {
                        mediaType = mediaType.withCharset(defaultCharset);
                    }
                } catch (IllegalArgumentException ignore) {}
            }

            resp.setContentType(mediaType.type() + "/" + mediaType.subtype());

            if (mediaType.charset().isPresent()) {
                resp.setCharacterEncoding(mediaType.charset().get().toString());
            }

            final ServletOutputStream output = resp.getOutputStream();
            try {
                output.write(asset.getResource());
            } finally {
                output.close();
            }
        } catch (RuntimeException ignored) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private static class AssetLoader extends CacheLoader<String, Asset> {
        private final String resourcePath;
        private final String uriPath;
        private final String indexFilename;
        private final Iterable<Map.Entry<String, String>> overrides;

        private AssetLoader(String resourcePath, String uriPath, String indexFilename, Iterable<Map.Entry<String, String>> overrides) {
            final String trimmedPath = CharMatcher.is('/').trimFrom(resourcePath);
            this.resourcePath = trimmedPath.isEmpty() ? trimmedPath : trimmedPath + "/";
            final String trimmedUri = CharMatcher.is('/').trimTrailingFrom(uriPath);
            this.uriPath = trimmedUri.length() == 0 ? "/" : trimmedUri;
            this.indexFilename = indexFilename;
            this.overrides = overrides;
        }

        @Override
        public Asset load(String key) throws Exception {
            Preconditions.checkArgument(key.startsWith(uriPath));

            Asset asset = loadOverride(key);
            if (asset != null) {
                return asset;
            }

            final String requestedResourcePath = CharMatcher.is('/').trimFrom(key.substring(uriPath.length()));
            final String absoluteRequestedResourcePath = CharMatcher.is('/').trimFrom(
                    this.resourcePath + requestedResourcePath);

            URL requestedResourceURL = Resources.getResource(absoluteRequestedResourcePath);

            if (ResourceURL.isDirectory(requestedResourceURL)) {
                if (indexFilename != null) {
                    requestedResourceURL = Resources.getResource(absoluteRequestedResourcePath + '/' + indexFilename);
                } else {
                    // directory requested but no index file defined
                    return null;
                }
            }

            long lastModified = ResourceURL.getLastModified(requestedResourceURL);
            if (lastModified < 1) {
                // Something went wrong trying to get the last modified time: just use the current time
                lastModified = System.currentTimeMillis();
            }

            // zero out the millis since the date we get back from If-Modified-Since will not have them
            lastModified = (lastModified / 1000) * 1000;
            return new StaticAsset(Resources.toByteArray(requestedResourceURL), lastModified);
        }

        private Asset loadOverride(String key) throws Exception {
            // TODO: Support prefix matches only for directories
            for (Map.Entry<String, String> override : overrides) {
                File file = null;
                if (override.getKey().equals(key)) {
                    // We have an exact match
                    file = new File(override.getValue());
                } else if (key.startsWith(override.getKey())) {
                    // This resource is in a mapped subdirectory
                    file = new File(override.getValue(), key.substring(override.getKey().length()));
                }

                if (file == null || !file.exists()) {
                    continue;
                }

                if (file.isDirectory()) {
                    file = new File(file, indexFilename);
                }

                if (file.exists()) {
                    return new FileSystemAsset(file);
                }
            }

            return null;
        }
    }

    private static interface Asset {
        byte[] getResource();
        String getETag();
        long getLastModifiedTime();
    }

    /** Weigh an asset according to the number of bytes it contains. */
    private static final class AssetSizeWeigher implements Weigher<String, Asset> {
        @Override
        public int weigh(String key, Asset asset) {
            return asset.getResource().length;
        }
    }

    /**
     * An asset implementation backed by the file-system.  If the backing file changes on disk, then this asset
     * will automatically reload its contents from disk.
     */
    private static class FileSystemAsset implements Asset {
        private final File file;
        private byte[] bytes;
        private String eTag;
        private long lastModifiedTime;

        public FileSystemAsset(File file) {
            this.file = file;
            refresh();
        }

        @Override
        public byte[] getResource() {
            maybeRefresh();
            return bytes;
        }

        @Override
        public String getETag() {
            maybeRefresh();
            return eTag;
        }

        @Override
        public long getLastModifiedTime() {
            maybeRefresh();
            return (lastModifiedTime / 1000) * 1000;
        }

        private synchronized void maybeRefresh() {
            if (lastModifiedTime != file.lastModified()) {
                refresh();
            }
        }

        private synchronized void refresh() {
            try {
                byte[] newBytes = Files.toByteArray(file);
                String newETag = Hashing.murmur3_128().hashBytes(newBytes).toString();

                bytes = newBytes;
                eTag = '"' + newETag + '"';
                lastModifiedTime = file.lastModified();
            } catch (IOException e) {
                // Ignored, don't update anything
            }
        }
    }

    /**
     * A static asset implementation.  This implementation just encapsulates the raw bytes of an asset (presumably
     * loaded from the classpath) and will never change.
     */
    private static class StaticAsset implements Asset {
        private final byte[] resource;
        private final String eTag;
        private final long lastModifiedTime;

        private StaticAsset(byte[] resource, long lastModifiedTime) {
            this.resource = resource;
            this.eTag = Hashing.murmur3_128().hashBytes(resource).toString();
            this.lastModifiedTime = lastModifiedTime;
        }

        public byte[] getResource() {
            return resource;
        }

        public String getETag() {
            return eTag;
        }

        public long getLastModifiedTime() {
            return lastModifiedTime;
        }
    }
}
