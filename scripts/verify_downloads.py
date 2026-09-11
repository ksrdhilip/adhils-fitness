"""Read-only integrity checks. Does not download, execute, or modify artifacts."""
from hashlib import sha256
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parent.parent
archive = ROOT / '.tools' / 'gradle-8.11.1-bin.zip'
# Publisher reference: https://gradle.org/release-checksums/#v8.11.1
expected = 'f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6'
actual = sha256(archive.read_bytes()).hexdigest()
if actual != expected:
    raise SystemExit('FAIL: Gradle archive differs from the published SHA-256.')
print('PASS: Gradle archive matches the publisher SHA-256:', actual)

count = 0
with ZipFile(archive) as package:
    for entry in package.infolist():
        if entry.is_dir():
            continue
        extracted = (ROOT / '.tools' / entry.filename).resolve()
        if not extracted.is_relative_to((ROOT / '.tools' / 'gradle-8.11.1').resolve()):
            raise SystemExit('FAIL: Unexpected archive path.')
        if not extracted.is_file() or sha256(extracted.read_bytes()).digest() != sha256(package.read(entry)).digest():
            raise SystemExit('FAIL: Extracted file differs from verified archive: ' + entry.filename)
        count += 1
print(f'PASS: All {count} extracted Gradle files match the verified archive.')

model = ROOT / 'app' / 'src' / 'main' / 'assets' / 'pose_landmarker_lite.task'
print('Pose model local SHA-256 (inventory, not independent publisher verification):', sha256(model.read_bytes()).hexdigest())
with ZipFile(model) as package:
    print('Pose model archive entries:', ', '.join(package.namelist()))
