# Compose 编译基线修正

实测原 BOM 2026.09.00 对应 Compose 1.12.1，多项 AAR metadata 明确要求 compileSdk >=37，与 Plan compileSdk36 冲突；见 docs/verification/domain-red.log。按稳定 API36 约束调整 BOM 为 2026.03.00（官方 compose-samples 有该版本发布），其实际依赖兼容性必须经构建验证；不改 targetSdk36/minSdk29。
