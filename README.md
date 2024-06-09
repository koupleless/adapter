<div align="center">

English | [简体中文](./README-zh_CN.md)

</div>

Koupleless Adapter 适配器的维护仓库, 注意 Adapter main 分支同时维护了 jdk8 和 jdk17 的代码，adapter 的 release 在 runtime 仓库的 [release github workflow](https://github.com/koupleless/runtime/actions/)。
jdk8 版本的 adapter 由 [runtime jdk8 的 release 脚本发布](https://github.com/koupleless/runtime/actions/workflows/koupleless_runtime_release.yml)，jdk17 版本的 adapter 由 [runtime jdk17 的 release 脚本发布](https://github.com/koupleless/runtime/actions/workflows/koupleless_runtime_release_2.1.x.yml)，
注意，由于 jdk8 版本和 jdk17 版本都会发布 koupleless-adapter 这个 parent bundle，导致发布完 jdk8 后再发布 jdk17 之后在上传到 maven central 仓库时，会因为 koupleless-adapter 已发布而失败。这时候只要在待发布的文件内容列表里删除 koupleless-adapter 这个文件夹就可以了。
