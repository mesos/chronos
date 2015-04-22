# Chronos Documentation Website

## Run it locally

Ensure you have installed everything listed in the dependencies section before
following the instructions.

### Dependencies

* [Bundler](http://bundler.io/)
* [Node.js](http://nodejs.org/) (for compiling assets)
* Python
* Ruby
* [RubyGems](https://rubygems.org/)

### Instructions

1. Install packages needed to generate the site

    * On Linux:

            $ apt-get install ruby-dev make autoconf nodejs nodejs-legacy python-dev
    * On Mac OS X:
    
            $ brew install node

2. Clone the Chronos repository

3. Change into the "docs" directory where docs live

        $ cd docs

4. Install Bundler

        $ gem install bundler

5. Install the bundle's dependencies

        $ bundle install

6. Start the web server

        $ bundle exec jekyll serve --watch

7. Visit the site at
   [http://localhost:4000/chronos/](http://localhost:4000/chronos/)

## Deploying the site

1. Clone a separate copy of the Chronos repo as a sibling of your normal
   Chronos project directory and name it "chronos-gh-pages".

        $ git clone git@github.com:mesos/chronos.git chronos-gh-pages

2. Check out the "gh-pages" branch.

        $ cd /path/to/chronos-gh-pages
        $ git checkout gh-pages

3. Copy the contents of the "docs" directory in master to the root of your
   chronos-gh-pages directory.

        $ cd /path/to/chronos
        $ cp -r docs/** ../chronos-gh-pages

4. Change to the chronos-gh-pages directory, commit, and push the changes

        $ cd /path/to/chronos-gh-pages
        $ git commit . -m "Syncing docs with master branch"
        $ git push
