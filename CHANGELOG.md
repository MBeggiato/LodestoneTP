# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning in a pragmatic form.

## [Unreleased]

### Added

- Placeholder for upcoming changes.

## [1.1] - 2026-03-06

### Changed

- Codebase cleanup
- Refactoring of the whole plugin while keeping it compatible with older versions

## [1.0] - 2026-03-04

### Added

- Initial public feature set for Lodestone-based teleporters on Paper 1.21.11+.
- SQLite-backed persistence with automated schema migrations.
- Dialog-based creation, management, and teleport selection workflows.
- Public/private teleporters, whitelist access, configurable costs, effects, and admin controls.
- Introduced a dedicated changelog for tracking future releases.
- Added a full in-game admin configuration flow for cooldowns, costs, effects, creation fees, and teleporter management.
- Added teleporter networks with optional permission nodes.
- Added favorites, usage-based sorting, linked teleporters, and per-teleporter cooldown overrides.
- Added configurable teleport warmup/channeling with movement cancel support.
- Added richer teleport effects, ambient effects, and configurable light blocks.
- Added custom advancements and progression rewards.

### Changed

- Cleaned up large dialog classes and reduced repeated dialog/button construction code.
- Split database responsibilities into dedicated support, migrator, and repository classes.
- Improved overall project structure and maintainability across dialogs, database access, and plugin bootstrap code.
- Updated the plugin version to `1.1`.

### Fixed

- Improved internal consistency around permission checks and admin bypass behavior.
- Reduced deprecated API usage and general code duplication.
- Validated the current codebase repeatedly with successful Gradle builds.
