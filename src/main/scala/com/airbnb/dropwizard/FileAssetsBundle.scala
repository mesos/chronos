package com.airbnb
package dropwizard

import com.yammer.dropwizard.assets.AssetServlet
import com.yammer.dropwizard.Bundle
import com.yammer.dropwizard.config.{Bootstrap, Environment}

/**
 * TODO(FL): Remove and bundle up.
 * @author Florian Leibert (flo@leibert.de)
 */
class FileAssetsBundle extends Bundle {

  def initialize(bootstrap : Bootstrap[_]) {
    // noop
  }

  def run(environment: Environment) {
    environment.addServlet(new AssetServlet("file:///tmp/web/app/", "/", "index.html"), "/*")
  }
}