package dev.vulnlog.dsl

public interface VlSuppressContext {
    public var templateFilename: String
    public var idMatcher: String
    public var template: String
}
