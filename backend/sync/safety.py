import logging
from typing import Dict, Any, Set

logger = logging.getLogger(__name__)

class SafetyValidationError(Exception):
    """Raised when safety validation checks fail."""
    pass

def validate_source_id(value: Any) -> bool:
    if value is None:
        return False
    val_str = str(value).strip()
    if not val_str or len(val_str) > 200:
        return False
    return True

def validate_sync_safety(
    discovered_servers: Dict[str, Any],
    previous_count: int,
    min_ratio: float = 0.90,
    allow_empty_override: bool = False
) -> None:
    current_count = len(discovered_servers)

    # Check 1: Non-zero count
    if current_count == 0:
        if not allow_empty_override:
            raise SafetyValidationError(
                "Safety Violation: Discovered 0 servers from source. Aborting sync to prevent mass deletion."
            )

    # Check 2: Valid non-empty source IDs (supports both numeric IDs and pvl_... string IDs)
    invalid_ids = []
    for sid in discovered_servers.keys():
        if not validate_source_id(sid):
            invalid_ids.append(sid)

    if invalid_ids:
        raise SafetyValidationError(
            f"Safety Violation: {len(invalid_ids)} invalid or empty source IDs found in source. Sample: {invalid_ids[:5]}"
        )

    # Check 3: Check for duplicate keys
    if len(discovered_servers.keys()) != len(set(discovered_servers.keys())):
        raise SafetyValidationError("Safety Violation: Duplicate source IDs detected in discovered servers.")

    # Check 4: Mass deletion threshold check
    if previous_count > 10 and current_count < (previous_count * min_ratio):
        if not allow_empty_override:
            raise SafetyValidationError(
                f"Safety Violation: Current count ({current_count}) dropped significantly below threshold ({int(previous_count * min_ratio)}) of previous count ({previous_count}). Aborting sync."
            )

    logger.info(
        f"Safety validation passed: source IDs valid={current_count} invalid=0 duplicates=0. "
        f"Server count: {current_count} (Previous: {previous_count})"
    )
