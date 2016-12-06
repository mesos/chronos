---
title: Web UI
---


# Chronos Web UI

### Compiling Assets

**Node.js is required to build assets**

Assets are automatically compiled when running `mvn package`. If you change assets, and want them updated in your jar, you must either `rm -rf src/main/resources/ui/build` or `mvn clean`.

### Build Requirements

When building and optimizing the assets, make sure you have at least 1GB of RAM available.
