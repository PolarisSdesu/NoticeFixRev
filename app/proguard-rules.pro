# libxposed entry point: keep the module class and its hook implementations.
-keep class moe.gbxin.noticefixrev.module.ModuleEntry { *; }
-keep class moe.gbxin.noticefixrev.module.** { *; }

# The framework reads this file to discover entry classes.
-adaptresourcefilecontents META-INF/xposed/java_init.list

# libxposed api is compile-only and provided at runtime.
-dontwarn io.github.libxposed.api.**

# Compose
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**
