# Commerce integration / 商业联动

The adapter is included in Cigarettes: Embers; it does not patch the Commerce JAR. Runtime tested with Commerce 1.6.29; the 1.6.30 source snapshot was checked as the API reference. Both mods are installed on server and clients for integration. Commerce is optional for standalone Cigarettes play.

## Cultivation certificates — 0.1.2

CommerceQualificationMixin adds the entry to the existing qualification screen using its scaled control layout. CultivationScreen preserves the certificate draft and selected standard restriction exemptions. CultivationNetwork sends the complete issuance request on the Cigarettes channel (protocol 12). The server checks tablet authority, current menu ID, menu validity, loaded position and dispenser identity before invoking Commerce's own validated issue method. Failure creates no privilege.

Overworld SavedData embers_cultivation_permits stores issued certificate IDs, not copied lifetime permissions. CultivationPermits queries QualificationCertificateSavedData.find and checks the current HolderUuid, Holder, ExpiresAt and Revoked fields. Unassigned claims retain the same ID and acquire the correct owner through Commerce; offline named holders are UUID-bound by Commerce on login. The client cannot grant placement rights. Expiry uses Commerce's real-time timestamp and is checked at planting; there is no expiry grace period. TobaccoSeed alone applies the biome waiver, so established TobaccoCrop blocks are unaffected and soil rules remain.

## Tablet and refilling

CommerceTabletMixin adds the existing Cigarettes world-settings page. PressureSettings is overworld SavedData; disabling stress clears only pressure-owned counters and effects. Brand benefits, addiction, mob gifting and villager discounts have separate switches. AdminNetwork validates administrator authority and the active tablet/station context.

CommerceBridge uses Commerce BankAccountSavedData.withdraw, creditTreasury and CommerceTransactionSavedData.record. A positive-price refill requires a depleted lighter and an empty output, consumes no lava, and creates the full-fuel output only after successful debit. Treasury and phone history receive the same fee. Repeated requests, occupied output and insufficient funds are rejected. Zero-price stations retain material refilling. No second wallet or account database is introduced.

## 中文

本联动全部位于香烟 JAR，无需修改商业模组。种植资格证沿用商业模组的玩家绑定、现实天数有效期和吊销记录；香烟只保存证件 ID，并在实际播种时从服务端验证。勾选、取消、返回以及原有资格证限制选项可同时使用。已经种植的烟草不因证件过期消失，土壤要求不豁免。

压力设置、收费充气、国库入账和手机流水沿用既有商业服务。实际测试版本为 Commerce 1.6.29；1.6.30源码用作接口参考，未知后续版本需要另行验证。
