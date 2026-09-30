import logging
from collections import defaultdict
from sync.source import MultiSourceHarvester
from sync.ovpn import OvpnDownloader

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("test_all_sources")

def run_test():
    harvester = MultiSourceHarvester()
    downloader = OvpnDownloader()

    logger.info("Harvesting from all multi-sources...")
    all_servers = harvester.harvest_all_sources()
    logger.info(f"Total harvested unique servers: {len(all_servers)}")

    by_source = defaultdict(list)
    for s in all_servers.values():
        by_source[s.source_name].append(s)

    logger.info("\n=== HARVESTED BY SOURCE ===")
    for src, list_s in sorted(by_source.items()):
        logger.info(f"Source: {src:<25} | Count: {len(list_s)}")

    logger.info("\n=== TESTING DOWNLOAD / PROFILE RESOLUTION (10 SERVERS PER SOURCE) ===")
    results_summary = {}

    for src, list_s in sorted(by_source.items()):
        sample = list_s[:10]
        success_count = 0
        fail_count = 0

        for s in sample:
            res = downloader.download_profile(s)
            if res.is_success and res.content and len(res.content) >= 20:
                success_count += 1
            else:
                fail_count += 1

        results_summary[src] = (success_count, fail_count, len(sample))
        logger.info(f"Source: {src:<25} | Tested: {len(sample)} | Success: {success_count} | Failed: {fail_count}")

    print("\n=======================================================")
    print("ALL SOURCES COMPREHENSIVE LOCAL TEST SUMMARY")
    print("=======================================================")
    for src, (succ, fail, total) in results_summary.items():
        pct = (succ / total) * 100 if total > 0 else 0
        print(f"Source: {src:<25} | {succ}/{total} Successful ({pct:.1f}%)")
    print("=======================================================")

if __name__ == "__main__":
    run_test()
