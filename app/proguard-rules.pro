-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization public class io.github.dx.xmprint.ModuleMain {
    public <init>();
}
