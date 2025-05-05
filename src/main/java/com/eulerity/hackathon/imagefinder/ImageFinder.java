package com.eulerity.hackathon.imagefinder;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@WebServlet(name = "ImageFinder", urlPatterns = {"/main"})
public class ImageFinder extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected static final Gson GSON = new GsonBuilder().create();

    public static final String[] testImages = {
            "https://www.unc.edu/",
            "https://newbrunswick.rutgers.edu",
            "https://www.netflix.com/",
            "https://news.mit.edu/"
    };

    private static final int MAX_PAGES = 50;
    private static final int MAX_THREADS = 10;
    private static final int TIMEOUT_MS = 5000;

    private Set<String> visited = ConcurrentHashMap.newKeySet();
    private ExecutorService executor;

    @Override
    public void init() throws ServletException {
        super.init();
        executor = Executors.newFixedThreadPool(MAX_THREADS);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String url = request.getParameter("url");
        if (url == null || url.isEmpty()) {
            out.print("[]");
            out.flush();
            return;
        }

        List<String> imageLinks = crawlPageForImages(url);
        out.print(GSON.toJson(imageLinks));
        out.flush();
    }

    private List<String> crawlPageForImages(String url) {
        Set<String> imageUrls = ConcurrentHashMap.newKeySet();
        visited.clear();

        CountDownLatch latch = new CountDownLatch(1);
        crawl(normalizeUrl(url), imageUrls, getDomain(url), latch);

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(imageUrls);
    }

    private void crawl(String url, Set<String> imageUrls, String domain, CountDownLatch parentLatch) {
        if (visited.size() >= MAX_PAGES || visited.contains(url)) {
            parentLatch.countDown();
            return;
        }
        visited.add(url);

        executor.submit(() -> {
            try {
                Thread.sleep(300);
//                Document doc = Jsoup.connect(url).timeout(TIMEOUT_MS).get();
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (compatible; ImageFinderBot/1.0; +https://your-site.com/bot-info)")
                        .timeout(TIMEOUT_MS)
                        .get();

                Elements images = doc.select("img");
                for (Element img : images) {
                    String src = normalizeUrl(img.absUrl("src"));
                    if (!src.isEmpty()) {
                        if (isLogo(src)) {
                            imageUrls.add(src + " [LOGO]");
                        } else {
                            imageUrls.add(src);
                        }
                    }
                }

                Elements links = doc.select("a[href]");
                List<String> nextLinks = new ArrayList<>();
                for (Element link : links) {
                    String nextUrl = normalizeUrl(link.absUrl("href"));
                    if (nextUrl.startsWith(domain) && !visited.contains(nextUrl)) {
                        nextLinks.add(nextUrl);
                    }
                }

                List<CountDownLatch> childLatches = new ArrayList<>();
                for (String nextUrl : nextLinks) {
                    if (visited.size() >= MAX_PAGES) {
                        break;
                    }
                    CountDownLatch childLatch = new CountDownLatch(1);
                    childLatches.add(childLatch);
                    crawl(nextUrl, imageUrls, domain, childLatch);
                }

                for (CountDownLatch latch : childLatches) {
                    latch.await();
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                parentLatch.countDown();
            }
        });
    }

    private String getDomain(String url) {
        try {
            URL u = new URL(url);
            return u.getProtocol() + "://" + u.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        try {
            URL u = new URL(url);
            String path = (u.getPath() != null) ? u.getPath() : "";
            String query = (u.getQuery() != null) ? ("?" + u.getQuery()) : "";
            String normalized = u.getProtocol() + "://" + u.getHost().toLowerCase() + path + query;
            if (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        } catch (MalformedURLException e) {
            return url;
        }
    }
    private boolean isLogo(String url) {
        String lower = url.toLowerCase();
        return lower.contains("logo") || lower.contains("brand") || lower.contains("header");
    }

}

