# Restriction Detector

A lightweight Android app that reads and displays the device policy restrictions currently enforced on your device — using root access for a complete, accurate view straight from the system.

## What it does

Restriction Detector runs `dumpsys device_policy` via root, scans the raw output for every `userRestriction_<name>` entry, and lists the restriction.

This is useful for anyone trying to understand exactly which policies (e.g. from a Device Owner, Profile Owner, or MDM solution) are currently active on their device.

## Features

- **One-tap scan** — tap "Grab Restrictions" to pull and parse the current restriction list
- **Root status indicator** — a clear green "Root granted" / red "Root not granted" label so you always know whether the scan can actually run
- **Copy to clipboard** — grab the full parsed list with one tap, ready to paste anywhere
- **Light/Dark toggle** — switch between day and night styling on demand, independent of system theme because why not :)

## How it works

1. On launch, the app checks for root access (`su -c id`) and shows the result at the top of the screen.
2. Tapping **Grab Restrictions** runs `su -c "dumpsys device_policy"` in the background.
3. The raw output is scanned with a regex for every `userRestriction_<name>` token; the prefix is stripped and each unique restriction name is listed on its own line, in order of first appearance.
4. Tapping **Copy all** copies the full list to the clipboard.

## Requirements

- A **rooted** Android device (Magisk, KernelSU, or equivalent)
- Root access granted to this app

## Scope

It only reads and reports the current policy state — it does not modify, bypass, enable, or disable any restriction. To bypass the restrictions, copy the list and use FuckDevicePolicy module to bypass the restrictions.
