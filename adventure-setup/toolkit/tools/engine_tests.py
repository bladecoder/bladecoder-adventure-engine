"""Compatibility commands delegate to project-defined shared runtime scenarios."""
from pathlib import Path
import uuid


def test_puzzle(root,config,puzzle):
    from runtime_scenarios import run_suite
    out=Path(root)/'build/runtime'/('puzzle-'+uuid.uuid4().hex)
    checks,_=run_suite(root,config,puzzle,out)
    return checks


def test_art(root,config,output):
    from runtime_scenarios import run_suite
    checks,_=run_suite(root,config,'art',output)
    return checks
