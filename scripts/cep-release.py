"""Generate immutable release feeds matching Morphe Manager's existing JSON protocol."""
import argparse
import datetime
import hashlib
import json
from pathlib import Path
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("directory", type=Path)
parser.add_argument("--run", required=True, type=int)
parser.add_argument("--sha", required=True)
parser.add_argument("--repo", required=True)
args = parser.parse_args()
base = f"https://github.com/{args.repo}/releases/download/cep-v1.0.{args.run}"
bundle = args.directory / "cep-patches.mpp"
with zipfile.ZipFile(bundle) as archive:
    assert archive.testzip() is None
    assert any(n.endswith(".dex") for n in archive.namelist()), "No Android DEX"
    manifest = archive.read("META-INF/MANIFEST.MF").decode().replace("\r\n ", "").replace("\n ", "")
    assert "Name: Cep Patches" in manifest, manifest
    assert "Patcher-Version:" in manifest, manifest
    assert any("WatchLaterShortcutsPatch" in n for n in archive.namelist()) or any(
        b"Watch later shortcuts" in archive.read(n) for n in archive.namelist() if n.endswith(".dex")
    ), "Shortcut patch absent"
# Remote sources deserialize created_at as kotlinx.datetime.LocalDateTime (no offset).
created = datetime.datetime.now(datetime.timezone.utc).replace(tzinfo=None).isoformat()
for filename, version, asset in [
    ("cep-patches-bundle.json", f"1.47.0-cep.{args.run}", "cep-patches.mpp"),
    ("app-release.json", f"1.0.{args.run}", "cep-patcher.apk"),
]:
    data = dict(created_at=created, page_url=base.replace('/releases/download/', '/releases/tag/'),
                description=f"Cep Patcher; source commit {args.sha}",
                download_url=f"{base}/{asset}", signature_download_url="", version=version)
    (args.directory / filename).write_text(json.dumps(data, indent=2) + "\n")
(args.directory / "build-info.json").write_text(json.dumps(dict(
    commit=args.sha, repository=args.repo, manager_upstream="da8545c92c94b83e23adf7b573b46b282fa2b062",
    patch_sha256=hashlib.sha256(bundle.read_bytes()).hexdigest(),
    application_id="io.github.b061raporlama.ceppatcher", created_at=created,
), indent=2) + "\n")
print("Validated DEX bundle and generated release feeds")
