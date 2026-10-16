# Cep Patcher source provenance

This directory is a modified source snapshot of https://github.com/MorpheApp/morphe-manager
at commit da8545c92c94b83e23adf7b573b46b282fa2b062 (upstream v1.35.0).
Original project history: https://github.com/MorpheApp/morphe-manager/commits/da8545c92c94b83e23adf7b573b46b282fa2b062
Morphe Manager builds on ReVanced Manager and Universal-ReVanced-Manager (URV).
Original copyright headers, LICENSE, NOTICE, library notices and credits are retained.

Modified 2026-10-16 by Cep Patcher contributors:
- Independent Cep Patcher name, pocket/clock vector artwork and application ID.
- Default expert mode; Cep Patches bundle embedded and seeded atomically on first use.
- Updates from this repository's Release JSON; no upstream patch API fallback.
- Own manager update feed; no upstream Firebase project or deep link registration.
- Bundled patch loader instrumentation test and combined build/release workflow.
- Modified 2026-10-16: migrate older embedded Cep bundles on APK upgrades without downgrading newer online bundles; migrate stored old Morphe YouTube clone identities for the built-in source.

This is an independent derivative, not an official Morphe release or endorsed project.
Manager and original artwork are distributed under GPL-3.0-only, with the upstream
Section 7 conditions in NOTICE. No upstream trademark rights are granted.
Full corresponding source is shipped in this repository and every APK release.
