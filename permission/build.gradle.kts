val versionOverride = project.findProperty("versionOverride") as? String
version = versionOverride ?: "local-SNAPSHOT"
