import os
import re
import urllib.request
import subprocess
import concurrent.futures

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)
LOADER_FILE = os.path.join(PROJECT_DIR, "shared/src/commonMain/kotlin/com/adhils/fitness/ExerciseImageLoader.kt")
VIDEOS_DIR = os.path.join(PROJECT_DIR, "videos")
CACHE_DIR = os.path.join(PROJECT_DIR, ".cache_gifs")

os.makedirs(VIDEOS_DIR, exist_ok=True)
os.makedirs(CACHE_DIR, exist_ok=True)

# Parse EXERCISE_ANIMATION_URLS from ExerciseImageLoader.kt
with open(LOADER_FILE, "r", encoding="utf-8") as f:
    content = f.read()

cdn_anim_base = "https://cdn.jsdelivr.net/gh/omercotkd/exercises-gifs@main/assets/"
cdn_gym_base = "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@v1.1.0/"

# Find entries like: "goblet-squat" to "${CDN_ANIM_BASE}1760.gif",
pattern = re.compile(r'"([a-zA-Z0-9_-]+)"\s*to\s*"\$\{(CDN_[A-Z_]+)\}([^"]+)"')
matches = pattern.findall(content)

items = []
for ex_id, cdn_var, path in matches:
    base = cdn_anim_base if cdn_var == "CDN_ANIM_BASE" else cdn_gym_base
    url = base + path
    items.append((ex_id, url))

# Also check for march (it was fixed earlier or handled)
# In ExerciseImageLoader: "march" is mapped to "Fast_Skipping/0.jpg" in image paths, let's check if march has an animation url
if "march" not in [i[0] for i in items]:
    items.append(("march", "https://cdn.jsdelivr.net/gh/omercotkd/exercises-gifs@main/assets/0598.gif"))

print(f"Total exercises found to convert: {len(items)}")

def process_exercise(item):
    ex_id, url = item
    output_mp4 = os.path.join(VIDEOS_DIR, f"{ex_id}.mp4")
    cached_gif = os.path.join(CACHE_DIR, f"{ex_id}.gif")
    
    if os.path.exists(output_mp4) and os.path.getsize(output_mp4) > 1000:
        return f"[SKIP] {ex_id}.mp4 already exists ({os.path.getsize(output_mp4)} bytes)"
    
    try:
        # Download GIF
        if not os.path.exists(cached_gif) or os.path.getsize(cached_gif) < 1000:
            headers = {'User-Agent': 'Mozilla/5.0'}
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=30) as resp, open(cached_gif, 'wb') as out_f:
                out_f.write(resp.read())
        
        # Convert to MP4 using ffmpeg
        # Filter ensures dimensions are even numbers (required by yuv420p / H.264)
        cmd = [
            "ffmpeg", "-y",
            "-i", cached_gif,
            "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2",
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-profile:v", "main",
            "-level", "3.1",
            "-movflags", "+faststart",
            "-an",
            output_mp4
        ]
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if res.returncode != 0:
            return f"[ERROR] ffmpeg failed for {ex_id}: {res.stderr.decode('utf-8', errors='ignore')[:200]}"
        
        size = os.path.getsize(output_mp4)
        return f"[OK] {ex_id}.mp4 ({size} bytes)"
    except Exception as e:
        return f"[ERROR] Failed for {ex_id}: {e}"

with concurrent.futures.ThreadPoolExecutor(max_workers=6) as executor:
    futures = [executor.submit(process_exercise, item) for item in items]
    for future in concurrent.futures.as_completed(futures):
        print(future.result())

print("All conversions complete!")
