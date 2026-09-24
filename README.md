# PublicVPNList OpenVPN Downloader

A professional, high-performance Python tool to discover and download OpenVPN profiles from `publicvpnlist.com` using only public web pages.

## Features

- **Dynamic Discovery**: Scans `sitemap.xml`, paginated lists, and country pages to find every available server.
- **Pure Web-Based**: No API keys, no `/api/v1/` calls, and no authentication bypasses.
- **Robust Downloader**: Handles temporary link generation, file verification, and country-based folder organization.
- **Smart Concurrency**: Multi-threaded downloads with configurable workers and rate-limiting.
- **Resilient**: Exponential backoff for `429 Too Many Requests`, automatic retries, and graceful `Ctrl+C` handling.
- **Resume Support**: Tracks state in `data/servers.json` and `logs/downloads.csv` to avoid redundant downloads.

## Installation

1. Ensure you have Python 3.11+ installed.
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Usage

### Basic Run
Discover all servers and start downloading:
```bash
python downloader.py
```

### Discovery Only
Find all server IDs without downloading:
```bash
python downloader.py --discover-only
```

### Fast Parallel Downloads
Use 10 workers for faster processing:
```bash
python downloader.py --workers 10
```

### Target Specific Country
Only download servers located in Japan:
```bash
python downloader.py --country Japan
```

### Testing
Test with a small limit of 5 servers:
```bash
python downloader.py --limit 5
```

## Options

| Flag | Description |
|------|-------------|
| `--workers N` | Number of concurrent downloaders (default: 6) |
| `--country NAME` | Filter by country name (e.g., Japan, "United States") |
| `--resume` | Resume from previously discovered servers in `data/servers.json` |
| `--discover-only` | Run discovery only and save to JSON |
| `--download-only` | Start downloading using existing JSON without re-scanning |
| `--limit N` | Limit discovery/downloading to N servers |
| `--yes` | Skip confirmation prompts |

## Project Structure

- `downloader.py`: Main application logic.
- `data/servers.json`: Current state of discovered servers.
- `output/`: Downloaded `.ovpn` files organized by country.
- `logs/downloads.csv`: Detailed log of every attempt.

## Disclaimer
This tool is for educational and personal use only. It respects the public structure of the website and implements rate limiting to avoid server strain.
