#!/usr/bin/env python3
"""Download royalty-free Wikimedia Commons stills into web/public/demo/."""

from __future__ import annotations

import json
import subprocess
import sys
import time
import urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "web/public/demo"
UA = "PrecisionFarmingDemo/1.0 (local demo; https://github.com/FilipeThome/precision-farming)"

# dest -> Commons filename (without File:)
PHOTOS: dict[str, str] = {
    "farms/farm-001.jpg": "Mato-grosso do sul plantio de soja - panoramio.jpg",
    "farms/farm-002.jpg": "Plantação de soja.jpg",
    "farms/farm-003.jpg": "Soy Production In Brazil (46200453114).jpg",
    "farms/farm-004.jpg": "Soja Tangará.jpg",
    "farms/farm-005.jpg": "Dourados MS milho (Valter Campanato)4set2009.jpg",
    "farms/farm-006.jpg": "LAVOURA DE SOJA QUERÊNCIA - panoramio.jpg",
    "farms/farm-007.jpg": "Soybeans no-till.jpg",
    "farms/farm-008.jpg": "Ao Fundo Lavoura de Algodão - panoramio.jpg",
    "crops/soja.jpg": "Soy beans.jpg",
    "crops/milho.jpg": "Corn on the cob.jpg",
    "crops/algodao.jpg": "Cotton boll nearly ready for harvest.jpg",
    "infra/silo.jpg": "Silos de armazenagem em Rio Pardo.JPG",
    "infra/warehouse.jpg": "SECADOR E ARMAZÉM DE GRÃOS - panoramio.jpg",
    "infra/inventory.jpg": "SECADOR E ARMAZÉM DE GRÃOS - panoramio.jpg",
    "infra/irrigation.jpg": "Center-pivot irrigation system - 2.jpg",
    "infra/pivot.jpg": "Center-pivot irrigation system - 1.jpg",
    "infra/drip.jpg": "Application of IrritecTape-drip-tape.jpg",
    "infra/sprinkler.jpg": "Travelling irrigation sprinkler 2 2014-05-29.jpg",
    "infra/pump.jpg": "Irrigation pump - geograph.org.uk - 183635.jpg",
    "infra/reservoir.jpg": "Irrigation Pond - geograph.org.uk - 124321.jpg",
    "infra/truck.jpg": "Fila de caminhões. Soja. - panoramio.jpg",
    "infra/soil.jpg": "Field soil sampling.jpg",
    "infra/weather.jpg": "ISS-20 Thunderstorms on the Brazilian Horizon.jpg",
    "infra/scouting.jpg": (
        "Byers, Texas farmer Tommy Henderson inspecting his wheat crop "
        "in his no-till field. (24999375852).jpg"
    ),
    "inventory/glyphosate.jpg": "Puerto Cruz 2022 – Glyphosate, Roundup.jpg",
    "inventory/urea.jpg": "Granular Urea application - geograph.org.uk - 1219037.jpg",
    "inventory/soy-seed.jpg": "Soybean Seed in Soil (9621475751).jpg",
    "inventory/diesel.jpg": "Dieselpumppu Monchegorskissa.jpg",
    "inventory/24d.jpg": "Tractor spraying pesticides IMG 5235.jpg",
    "inventory/oil-filter.jpg": "Engine oil filter cutaway.JPG",
    "inventory/corn-seed.jpg": "Zea mays seeds closeup.jpg",
    "inventory/map.jpg": "DAP (Diammonium Phosphate) Granules (3).jpg",
    "inventory/insecticide.jpg": "Pesticide spraying in spring.jpg",
    "inventory/kcl.jpg": "Compacted potassium chloride, fertilizer grade.jpg",
    "inventory/drive-belt.jpg": "Optibelt Kraftbänder.jpg",
    "inventory/cotton-seed.jpg": "Seeds of cotton.jpg",
    "inventory/pre-emergent.jpg": "Crop Spraying - geograph.org.uk - 445532.jpg",
    "inventory/hydraulic-oil.jpg": "Barrel Transfer or Drum Pumps pic2.JPG",
}


def curl(url: str, dest: Path | None = None) -> subprocess.CompletedProcess[bytes]:
    args = ["curl", "-fsSL", "--max-time", "45", "-A", UA]
    if dest:
        dest.parent.mkdir(parents=True, exist_ok=True)
        args += ["-o", str(dest), url]
        return subprocess.run(args, capture_output=True)
    args.append(url)
    return subprocess.run(args, capture_output=True)


def thumb_url(title: str) -> str | None:
    q = urllib.parse.urlencode(
        {
            "action": "query",
            "titles": f"File:{title}",
            "prop": "imageinfo",
            "iiprop": "url|mime|size",
            "iiurlwidth": "1280",
            "format": "json",
        }
    )
    res = curl(f"https://commons.wikimedia.org/w/api.php?{q}")
    if res.returncode != 0:
        return None
    try:
        pages = json.loads(res.stdout)["query"]["pages"]
    except (json.JSONDecodeError, KeyError):
        return None
    page = next(iter(pages.values()))
    if "missing" in page or not page.get("imageinfo"):
        return None
    info = page["imageinfo"][0]
    return info.get("thumburl") or info.get("url")


def download(dest: str, title: str) -> bool:
    path = ROOT / dest
    time.sleep(0.4)
    url = thumb_url(title)
    if not url:
        print(f"MISS {dest} {title}", flush=True)
        return False
    tmp = path.with_suffix(path.suffix + ".part")
    res = curl(url, tmp)
    if res.returncode != 0 or not tmp.exists() or tmp.stat().st_size < 8000:
        print(f"FAIL {dest} {title} size={tmp.stat().st_size if tmp.exists() else 0}", flush=True)
        if tmp.exists():
            tmp.unlink()
        return False
    tmp.replace(path)
    print(f"OK {dest} <- {title} ({path.stat().st_size} bytes)", flush=True)
    return True


def main() -> int:
    failed: list[str] = []
    for dest, title in PHOTOS.items():
        if not download(dest, title):
            failed.append(dest)
    print(json.dumps({"failed": failed}, indent=2, ensure_ascii=False), flush=True)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
