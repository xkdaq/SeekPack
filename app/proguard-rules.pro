# Project specific R8 rules.
#
# 注意：不要再加入 -dontshrink / -dontoptimize / -dontobfuscate，也不要大范围 -keep 整个包，
# 否则 Play Console 的「DEX 代码优化」得分会降为「低」。
# Activity / Application / View 构造函数、Parcelable CREATOR、枚举、native 方法、
# onClick 等规则已由 AGP 自动生成（aapt 规则 + proguard-android-optimize.txt），无需手写。
# 参考：https://developer.android.com/topic/performance/app-optimization/enable-app-optimization

# 输出映射文件用于还原崩溃堆栈（AGP 默认也会在 build/outputs/mapping 下生成）
-printmapping map.txt

# 保留行号，方便 Play Console 反混淆崩溃堆栈；源文件名统一替换
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 把所有混淆后的类重新打包到同一个包中（对应 Play Console「重新打包类」）
-repackageclasses

-dontwarn org.apache.**
