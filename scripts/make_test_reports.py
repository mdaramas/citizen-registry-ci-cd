#!/usr/bin/env python3
"""Render Maven Surefire/Failsafe JUnit XML as readable HTML and a GitHub job summary.

Uses only the Python standard library, so it runs on a GitHub-hosted Ubuntu runner.
Never invents results: an absent XML report is identified as 'NOT RUN'.
"""

from __future__ import annotations

import html
import os
from pathlib import Path
import xml.etree.ElementTree as ET


REPORTS = [
    ("Unit tests (JUnit / Surefire)", Path("citizen-service/target/surefire-reports"), "unit-tests"),
    ("API / Integration tests (Rest-Assured / Failsafe)", Path("citizen-integration-tests/target/failsafe-reports"), "api-integration-tests"),
]


def get_results(directory: Path) -> tuple[list[dict], list[dict]]:
    suites = []
    cases = []
    for report_file in sorted(directory.glob("TEST-*.xml")):
        tree = ET.parse(report_file)
        root = tree.getroot()
        suite_elements = [root] if root.tag == "testsuite" else root.findall("testsuite")
        for suite in suite_elements:
            rows = suite.findall("testcase")
            suites.append({"file": report_file.name, "name": suite.get("name", report_file.stem)})
            for case in rows:
                status = "PASS"
                failure = case.find("failure")
                error = case.find("error")
                skipped = case.find("skipped")
                detail = ""
                if failure is not None or error is not None:
                    issue = failure if failure is not None else error
                    status = "FAIL" if failure is not None else "ERROR"
                    detail = (issue.get("message") or issue.text or "")[:500]
                elif skipped is not None:
                    status = "SKIP"
                cases.append({
                    "class": case.get("classname", suite.get("name", "")),
                    "name": case.get("name", ""),
                    "time": case.get("time", "0"),
                    "status": status,
                    "detail": detail,
                })
    return suites, cases


def make_html(title: str, suites: list[dict], cases: list[dict]) -> str:
    count = len(cases)
    passed = sum(c["status"] == "PASS" for c in cases)
    failed = sum(c["status"] in ("FAIL", "ERROR") for c in cases)
    skipped = sum(c["status"] == "SKIP" for c in cases)
    rows = "\n".join(
        "<tr><td>{}</td><td>{}</td><td class='{}'>{}</td><td>{}</td><td>{}</td></tr>".format(
            html.escape(c["class"]), html.escape(c["name"]),
            c["status"].lower(), c["status"], html.escape(c["time"]),
            html.escape(c["detail"]),
        ) for c in cases
    ) or "<tr><td colspan='5'>NOT RUN: no JUnit XML files found.</td></tr>"
    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)} – Citizen Registry</title>
<style>
body{{font-family:system-ui,-apple-system,sans-serif;max-width:1200px;margin:32px auto;padding:0 18px;color:#172033;background:#fbfcfe}}
h1{{font-size:1.8rem}}.summary{{font-size:1.1rem;margin:22px 0;padding:16px;background:#eef3f9;border-radius:10px}}
table{{border-collapse:collapse;width:100%;font-size:.92rem;background:white}}th,td{{border:1px solid #dce3ec;padding:9px;text-align:left;vertical-align:top;word-break:break-word}}
th{{background:#e9eff7}}.pass{{color:#16703e;font-weight:700}}.fail,.error{{color:#b42318;font-weight:700}}.skip{{color:#7b5e18;font-weight:700}}
</style></head><body><h1>{html.escape(title)}</h1>
<p>Citizen Registry · Generated from genuine Maven JUnit XML output</p>
<div class="summary">Total: <strong>{count}</strong> · Passed: <strong>{passed}</strong> · Failed/errors: <strong>{failed}</strong> · Skipped: <strong>{skipped}</strong> · Suites: <strong>{len(suites)}</strong></div>
<table><thead><tr><th>Class</th><th>Test</th><th>Result</th><th>Seconds</th><th>Details</th></tr></thead><tbody>{rows}</tbody></table>
</body></html>"""


def main() -> None:
    destination = Path("ci-reports")
    destination.mkdir(exist_ok=True)
    summary = ["## Citizen Registry – automated test results", "", "| Test category | Total | Passed | Failed/errors | Skipped |", "|---|---:|---:|---:|---:|"]
    for title, directory, filename in REPORTS:
        suites, cases = get_results(directory)
        passed = sum(c["status"] == "PASS" for c in cases)
        failed = sum(c["status"] in ("FAIL", "ERROR") for c in cases)
        skipped = sum(c["status"] == "SKIP" for c in cases)
        (destination / (filename + ".html")).write_text(make_html(title, suites, cases), encoding="utf-8")
        summary.append(f"| {title} | {len(cases)} | {passed} | {failed} | {skipped} |")
        if not suites:
            summary.append(f"\n> No XML reports found for {title}; these tests did not produce a report.\n")
    summary.extend(["", "Full XML reports and readable HTML reports are available under **Artifacts** in this workflow run.", ""])
    markdown = "\n".join(summary)
    (destination / "summary.md").write_text(markdown, encoding="utf-8")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as output:
            output.write(markdown + "\n")
    print(markdown)


if __name__ == "__main__":
    main()
