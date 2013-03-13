package com.airbnb.dropwizard.assets

import com.bazaarvoice.dropwizard.assets.{AssetsConfiguration, AssetsBundleConfiguration}
import com.yammer.dropwizard.config.{Bootstrap, Environment}
import com.google.common.cache.CacheBuilderSpec
import com.yammer.dropwizard.ConfiguredBundle

/**
 * @author andykram (andy.kram@airbnb.com)
 */

object ConfiguredAssetsBundle {
  def appendTrailingSlash(v : String) : String = {
    v.last match {
      case '/' => v
      case _   => v + '/'
    }
  }

  val defaultCacheSpec : CacheBuilderSpec = CacheBuilderSpec.parse("maximumSize=100")
  val defaultIndexFile : String = "index.html"
}

class ConfiguredAssetsBundle(
  _resourcePath : String,
  _uriPath : String,
  indexFile : String = ConfiguredAssetsBundle.defaultIndexFile,
  cacheBuilderSpec : CacheBuilderSpec = ConfiguredAssetsBundle.defaultCacheSpec
) extends ConfiguredBundle[AssetsBundleConfiguration] {

  val resourcePath = ConfiguredAssetsBundle.appendTrailingSlash(_resourcePath)
  val uriPath = ConfiguredAssetsBundle.appendTrailingSlash(_uriPath)

  def initialize(bootstrap: Bootstrap[_]) {}

  def run(bundleConfig : AssetsBundleConfiguration, env : Environment) {
    val config : AssetsConfiguration = bundleConfig.getAssetsConfiguration


    // Let the cache spec from the configuration override the one specified in the code

    val spec : CacheBuilderSpec = config.getCacheSpec match {
      case s:String => CacheBuilderSpec.parse(s)
      case null => ConfiguredAssetsBundle.defaultCacheSpec
    }

    val overrides = config.getOverrides
    val assetServlet = new AssetServlet(resourcePath, spec, uriPath, indexFile, overrides)
    env.addServlet(assetServlet, uriPath + "*")
  }
}