from pathlib import Path

root = Path(r"f:/workspace/precision_farming/backend")
for yml in root.rglob("application.yml"):
    text = yml.read_text(encoding="utf-8")
    if "jwt-public-key:" not in text or "allow-demo-secrets" in text:
        continue
    lines = text.splitlines(keepends=True)
    out = []
    for line in lines:
        out.append(line)
        if "jwt-public-key:" in line:
            indent = line[: len(line) - len(line.lstrip())]
            out.append(f"{indent}allow-demo-secrets: ${{ALLOW_DEMO_SECRETS:true}}\n")
    yml.write_text("".join(out), encoding="utf-8")
    print("patched", yml)
