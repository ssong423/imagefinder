# Eulerity Image Finder
A multithreaded Java web crawler that extracts and displays image URLs from a given website — with features like logo detection, client-side filtering, and a user-friendly interface.

Originally developed as part of a coding challenge.

## Demo Screenshot

Here’s how the Image Finder works in action:

![App Demo](Screenshot%202025-05-05%20at%2016.13.29.png)

## Features
### Web Crawler (Java + JSoup)
Recursively crawls up to 50 pages within the same domain.

Extracts all <img> elements and collects their src URLs.

Skips disallowed paths by reading robots.txt.

### Crawl-Friendly Behavior
Adds a short delay (Thread.sleep(300)) between requests to reduce load on servers.

Uses a custom User-Agent header to identify the bot.

### Logo Detection
Detects potential logos based on:

alt attributes (e.g., "alt='logo'")

Image filenames containing “logo”

Marks them visually in the UI with a green [LOGO] badge.

### Client-Side Filtering (HTML + JS)
Lets users filter results in real time by keyword (e.g., logo, .svg, app-store).

Responsive search UI built with plain JavaScript.

### Frontend
Clean, responsive interface built with HTML/CSS/JS.

Displays image previews and download links for each image.

## Tech Stack
Backend: Java, JSoup, Servlet API, Maven, Jetty

Frontend: HTML, CSS, JavaScript

Concurrency: ExecutorService, CountDownLatch

