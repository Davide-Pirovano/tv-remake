#!/usr/bin/env python3
"""Create private first-install PIN credentials; never print or commit them."""
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import tempfile


TARGET = Path(__file__).resolve().parents[1] / "app/src/main/assets/owner.json"


def configure(pin, confirmation, target=TARGET):
    if not re.fullmatch(r"[0-9]{6,8}", pin):
        raise ValueError("Il PIN deve contenere da 6 a 8 cifre ASCII.")
    if pin != confirmation:
        raise ValueError("I PIN non coincidono.")
    if target.exists():
        raise ValueError("owner.json esiste già: spostalo per configurare nuove installazioni.")
    salt = secrets.token_bytes(16)
    payload = {
        "salt": salt.hex(),
        "hash": hashlib.pbkdf2_hmac("sha256", pin.encode("ascii"), salt, 60000, 32).hex(),
    }
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", dir=target.parent, delete=False) as output:
            temporary = Path(output.name)
            json.dump(payload, output)
            output.write("\n")
        os.chmod(temporary, 0o600)
        # A hard link fails if another configuration appeared meanwhile.
        os.link(temporary, target)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


if __name__ == "__main__":
    try:
        configure(getpass.getpass("PIN iniziale (6–8 cifre): "), getpass.getpass("Conferma PIN: "))
    except (ValueError, OSError) as error:
        raise SystemExit(str(error))
    print("Credenziali iniziali create nel file privato owner.json, escluso da Git.")
    print("Il PIN di un TV già configurato non viene modificato.")
