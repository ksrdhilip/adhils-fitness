import os
import sys
import urllib.request
import subprocess
from concurrent.futures import ThreadPoolExecutor

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)
VIDEOS_DIR = os.path.join(PROJECT_DIR, "videos")
CACHE_DIR = os.path.join(PROJECT_DIR, ".cache_exercises")

os.makedirs(VIDEOS_DIR, exist_ok=True)
os.makedirs(CACHE_DIR, exist_ok=True)

# 68 exercises mapped to real human photos in yuhonas/free-exercise-db
EXERCISE_MAPPING = {
    'arm-circles': 'Arm_Circles',
    'arnold-press': 'Arnold_Dumbbell_Press',
    'band-chest-press': 'Bench_Press_-_With_Bands',
    'band-curl': 'Close-Grip_EZ-Bar_Curl_with_Band',
    'band-face-pull': 'Face_Pull',
    'band-lateral-raise': 'Lateral_Raise_-_With_Bands',
    'band-press': 'Bench_Press_-_With_Bands',
    'band-pull-apart': 'Band_Pull_Apart',
    'band-pushdown': 'Triceps_Pushdown',
    'band-rdl': 'Stiff-Legged_Dumbbell_Deadlift',
    'band-row': 'Upright_Row_-_With_Bands',
    'band-squat': 'Box_Squat_with_Bands',
    'bench-dip': 'Bench_Dips',
    'bench-press': 'Dumbbell_Bench_Press',
    'bent-over-row': 'Bent_Over_Barbell_Row',
    'bird-dog': 'Superman',
    'bodyweight-squat': 'Bodyweight_Squat',
    'bridge': 'Barbell_Glute_Bridge',
    'bulgarian-split-squat': 'Split_Squat_with_Dumbbells',
    'calf-raise': 'Standing_Dumbbell_Calf_Raise',
    'cat-cow': 'Cat_Stretch',
    'chest-supported-row': 'Dumbbell_Incline_Row',
    'childs-pose': 'Childs_Pose',
    'curl': 'Dumbbell_Alternate_Bicep_Curl',
    'dead-bug': 'Dead_Bug',
    'dumbbell-split-squat': 'Split_Squat_with_Dumbbells',
    'dumbbell-swing': 'One-Arm_Kettlebell_Swings',
    'floor-press': 'Floor_Press',
    'front-raise': 'Front_Dumbbell_Raise',
    'goblet-squat': 'Goblet_Squat',
    'good-morning': 'Good_Morning',
    'hammer-curl': 'Alternate_Hammer_Curl',
    'hip-openers': 'Standing_Hip_Circles',
    'hip-thrust': 'Barbell_Hip_Thrust',
    'hollow-hold': 'Plank',
    'inchworm': 'Inchworm',
    'incline-curl': 'Alternate_Incline_Dumbbell_Curl',
    'incline-press': 'Incline_Dumbbell_Press',
    'incline-pushup': 'Incline_Push-Up',
    'knee-pushup': 'Pushups',
    'lateral-raise': 'Seated_Side_Lateral_Raise',
    'march': 'Fast_Skipping',
    'mountain-climber': 'Mountain_Climbers',
    'pallof-press': 'Pallof_Press',
    'pike-pushup': 'Pushups',
    'plank': 'Plank',
    'press': 'Dumbbell_Shoulder_Press',
    'pushup': 'Pushups',
    'rdl': 'Romanian_Deadlift',
    'reverse-fly': 'Barbell_Rear_Delt_Row',
    'reverse-lunge': 'Dumbbell_Rear_Lunge',
    'row': 'One-Arm_Dumbbell_Row',
    'russian-twist': 'Russian_Twist',
    'shrug': 'Barbell_Shrug',
    'side-plank': 'Push_Up_to_Side_Plank',
    'single-leg-bridge': 'Single_Leg_Glute_Bridge',
    'single-leg-rdl': 'Stiff-Legged_Dumbbell_Deadlift',
    'step-up': 'Barbell_Step_Ups',
    'suitcase-hold': 'Farmers_Walk',
    'sumo-squat': 'Plie_Dumbbell_Squat',
    'superman-pull': 'Superman',
    'thoracic-rotation': 'Worlds_Greatest_Stretch',
    'tricep-extension': 'Cable_Incline_Triceps_Extension',
    'tricep-kickback': 'Cable_One_Arm_Tricep_Extension',
    'wall-pushup': 'Pushups',
    'wall-sit': 'Bodyweight_Squat',
    'weighted-calf-raise': 'Barbell_Seated_Calf_Raise',
    'worlds-greatest-stretch': 'Worlds_Greatest_Stretch'
}

BASE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

def download_frame(folder, frame_idx):
    local_path = os.path.join(CACHE_DIR, f"{folder}_{frame_idx}.jpg")
    if not os.path.exists(local_path) or os.path.getsize(local_path) < 1000:
        url = f"{BASE_URL}{folder}/{frame_idx}.jpg"
        headers = {'User-Agent': 'Mozilla/5.0'}
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=30) as resp, open(local_path, 'wb') as f:
            f.write(resp.read())
    return local_path

def generate_video(ex_id):
    folder = EXERCISE_MAPPING[ex_id]
    output_mp4 = os.path.join(VIDEOS_DIR, f"{ex_id}.mp4")
    
    try:
        f0 = download_frame(folder, 0)
        f1 = download_frame(folder, 1)
        
        # Build 3.2s ping-pong seamless loop MP4 with 720x480 dark bg
        cmd = [
            "ffmpeg", "-y",
            "-loop", "1", "-t", "1.4", "-i", f0,
            "-loop", "1", "-t", "1.4", "-i", f1,
            "-loop", "1", "-t", "1.4", "-i", f0,
            "-filter_complex",
            "[0:v]scale=720:480:force_original_aspect_ratio=decrease,pad=720:480:(ow-iw)/2:(oh-ih)/2:color=0x14151C,setsar=1,fps=30[v0]; "
            "[1:v]scale=720:480:force_original_aspect_ratio=decrease,pad=720:480:(ow-iw)/2:(oh-ih)/2:color=0x14151C,setsar=1,fps=30[v1]; "
            "[2:v]scale=720:480:force_original_aspect_ratio=decrease,pad=720:480:(ow-iw)/2:(oh-ih)/2:color=0x14151C,setsar=1,fps=30[v2]; "
            "[v0][v1]xfade=transition=fade:duration=0.5:offset=0.9[x0]; "
            "[x0][v2]xfade=transition=fade:duration=0.5:offset=1.8[outv]",
            "-map", "[outv]",
            "-c:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-movflags", "+faststart",
            "-an",
            output_mp4
        ]
        
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if res.returncode != 0:
            return f"[ERROR] {ex_id}: {res.stderr.decode()[-200:]}"
        
        size_kb = os.path.getsize(output_mp4) / 1024.0
        return f"[OK] {ex_id} -> {folder} ({size_kb:.1f} KB)"
    except Exception as e:
        return f"[EXCEPTION] {ex_id}: {e}"

def main():
    print(f"Generating realistic human videos for {len(EXERCISE_MAPPING)} exercises...")
    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(generate_video, sorted(EXERCISE_MAPPING.keys())))
    
    for r in results:
        print(r)
    
    print("\nAll realistic human videos successfully generated!")

if __name__ == "__main__":
    main()
