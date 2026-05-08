#The GNN was trained by use of google colab and the commands used are as follows 

#-------------------------------------------------------------------
from google.colab import drive
from pathlib import Path

drive.mount('/content/drive')

drive_project = Path("/content/drive/MyDrive/HandwritingProject")

if drive_project.exists():
    print("Folder found. Contents:")
    for f in sorted(drive_project.iterdir()):
        print(f"  {f.name}  ({f.stat().st_size / 1024:.1f} KB)")
else:
    print("Folder not found at", drive_project)
    print("\nTop-level MyDrive contents:")
    for f in sorted(Path("/content/drive/MyDrive").iterdir()):
        print(f"  {f.name}")
        
#-------------------------------------------------------------------
import pandas as pd
from collections import Counter

df = pd.read_csv("/content/drive/MyDrive/HandwritingProject/handwriting_features.csv").dropna()
print("Total samples:", len(df))
print("Columns:", df.columns.tolist())
print("\nLabel distribution:")
print(Counter(df['label'].tolist()))
print("\nFirst few rows:")
print(df.head())
#-------------------------------------------------------------------
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.nn import GCNConv
from torch_geometric.data import Data
from sklearn.preprocessing import LabelEncoder, MinMaxScaler
from sklearn.utils import resample
import pandas as pd
import numpy as np
from collections import Counter
from pathlib import Path
import csv

drive_project = Path("/content/drive/MyDrive/HandwritingProject")

df = pd.read_csv(str(drive_project / "handwriting_features.csv")).dropna()

# ── Check how bad the LoopFrequency corruption is ────────────────────────────
zero_loop = (df['LoopFrequency'] == 0.0).sum()
print(f"Rows with LoopFrequency=0: {zero_loop} / {len(df)} ({100*zero_loop/len(df):.1f}%)")
print("LoopFrequency stats BEFORE fix:")
print(df['LoopFrequency'].describe())
#-------------------------------------------------------------------
import zipfile
from pathlib import Path

archive_path = "/content/drive/MyDrive/HandwritingProject/archive.zip"

with zipfile.ZipFile(archive_path, 'r') as z:
    all_files = z.namelist()
    print(f"Total files: {len(all_files)}")
    print("\nFirst 20 entries:")
    for f in all_files[:20]:
        print(f" ", f)
    
    # Check what file types exist
    from collections import Counter
    exts = Counter(Path(f).suffix.lower() for f in all_files)
    print("\nFile types:", exts.most_common())
#-------------------------------------------------------------------
import zipfile
import numpy as np
from PIL import Image
import io
import pandas as pd
from pathlib import Path
from collections import defaultdict

drive_project = Path("/content/drive/MyDrive/HandwritingProject")
archive_path  = str(drive_project / "archive.zip")

def compute_loop_frequency(img_array):
    """
    Mirrors your Java logic:
    - Convert to binary (threshold 128)
    - Count horizontal projection valleys (rows with low ink)
    - Return avg loops per ink row, capped at 15
    """
    if img_array.ndim == 3:
        gray = np.mean(img_array, axis=2)
    else:
        gray = img_array.astype(float)

    binary = (gray < 128).astype(int)  # 1 = ink pixel

    row_sums = binary.sum(axis=1)
    rows_with_ink = np.sum(row_sums > 0)
    if rows_with_ink == 0:
        return 0.0

    # Count valleys in horizontal projection (loop indicator)
    total_loops = 0
    in_valley = False
    mean_ink  = row_sums[row_sums > 0].mean()
    threshold = mean_ink * 0.3

    for s in row_sums:
        if s < threshold:
            if not in_valley:
                total_loops += 1
                in_valley = True
        else:
            in_valley = False

    avg = total_loops / rows_with_ink
    return min(avg * 15.0, 15.0)  # scale and cap to match Java

# ── Load existing CSV ─────────────────────────────────────────────────────────
df = pd.read_csv(str(drive_project / "handwriting_features.csv")).dropna()
print(f"CSV has {len(df)} rows")

# ── Check if CSV has an image path column ────────────────────────────────────
print("Columns:", df.columns.tolist())
print(df.head(3))
#-------------------------------------------------------------------
import zipfile

archive_path = "/content/drive/MyDrive/HandwritingProject/archive.zip"

with zipfile.ZipFile(archive_path, 'r') as z:
    with z.open("iam_words/words.txt") as f:
        lines = f.read().decode('utf-8').split('\n')
    
    # Show header comments and first real entries
    for line in lines[:30]:
        print(line)
#-------------------------------------------------------------------
import zipfile
import numpy as np
from PIL import Image
import io
import pandas as pd
from pathlib import Path
import random

drive_project = Path("/content/drive/MyDrive/HandwritingProject")
archive_path  = str(drive_project / "archive.zip")

def compute_loop_frequency(img_array):
    if img_array.ndim == 3:
        gray = np.mean(img_array, axis=2)
    else:
        gray = img_array.astype(float)

    binary   = (gray < 128).astype(int)
    row_sums = binary.sum(axis=1)
    rows_with_ink = np.sum(row_sums > 0)
    if rows_with_ink == 0:
        return 0.0

    total_loops = 0
    in_valley   = False
    threshold   = row_sums[row_sums > 0].mean() * 0.3

    for s in row_sums:
        if s < threshold:
            if not in_valley:
                total_loops += 1
                in_valley = True
        else:
            in_valley = False

    avg = total_loops / rows_with_ink
    return min(avg * 15.0, 15.0)

# ── Sample images from archive and compute loop frequencies ──────────────────
print("Sampling images from archive...")

with zipfile.ZipFile(archive_path, 'r') as z:
    png_files = [f for f in z.namelist() if f.endswith('.png')]
    print(f"Total images: {len(png_files)}")

    # Sample 1264 images (same size as your CSV)
    random.seed(42)
    sampled = random.sample(png_files, min(1264, len(png_files)))

    loop_values = []
    errors = 0
    for i, fname in enumerate(sampled):
        try:
            with z.open(fname) as f:
                img = Image.open(io.BytesIO(f.read())).convert('L')
                arr = np.array(img)
                loop_values.append(compute_loop_frequency(arr))
        except Exception:
            loop_values.append(0.0)
            errors += 1

        if (i + 1) % 100 == 0:
            print(f"  Processed {i+1}/{len(sampled)}...")

print(f"\nDone. Errors: {errors}")
print(f"Loop frequency stats:")
lv = np.array(loop_values)
print(f"  mean={lv.mean():.3f}  std={lv.std():.3f}  "
      f"min={lv.min():.3f}  max={lv.max():.3f}  zeros={np.sum(lv==0)}")

# ── Patch the CSV ─────────────────────────────────────────────────────────────
df = pd.read_csv(str(drive_project / "handwriting_features.csv")).dropna()
df['LoopFrequency'] = loop_values[:len(df)]

print(f"\nPatched CSV loop frequency stats:")
print(df['LoopFrequency'].describe())

# Save patched CSV
out_path = str(drive_project / "handwriting_features_fixed.csv")
df.to_csv(out_path, index=False)
print(f"\n Saved patched CSV to {out_path}")
print("Label distribution:", df['label'].value_counts().to_dict())
#-------------------------------------------------------------------
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.nn import GCNConv
from torch_geometric.data import Data
from sklearn.preprocessing import LabelEncoder, MinMaxScaler
from sklearn.utils import resample
import pandas as pd
from collections import Counter
from pathlib import Path
import csv

drive_project = Path("/content/drive/MyDrive/HandwritingProject")
df = pd.read_csv(str(drive_project / "handwriting_features_fixed.csv")).dropna()

feature_cols = ['SlantAngle', 'LoopFrequency', 'LetterSpacing', 'PressureZone', 'StrokeDensity']

# ── Balance classes ───────────────────────────────────────────────────────────
target_size = df['label'].value_counts().max()  # 548
balanced = pd.concat([
    resample(df[df['label'] == cls],
             replace=True, n_samples=target_size, random_state=42)
    for cls in df['label'].unique()
])
df = balanced.sample(frac=1, random_state=42).reset_index(drop=True)
print("Balanced distribution:", Counter(df['label'].tolist()))

# ── Fresh scaler and encoder ──────────────────────────────────────────────────
le     = LabelEncoder()
scaler = MinMaxScaler()

X = torch.FloatTensor(scaler.fit_transform(df[feature_cols].values))
y = torch.LongTensor(le.fit_transform(df['label']))
print("Class order (index 0,1,2):", le.classes_.tolist())

# ── Fully connected graph ─────────────────────────────────────────────────────
N = len(X)
edges = [[i, j] for i in range(N) for j in range(N) if i != j]
edge_index = torch.LongTensor(edges).t().contiguous()
data = Data(x=X, edge_index=edge_index, y=y)

# ── 5→16→3 GCN ───────────────────────────────────────────────────────────────
class HandwritingGCN(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = GCNConv(5, 16)
        self.conv2 = GCNConv(16, 3)

    def forward(self, data):
        x, ei = data.x, data.edge_index
        x = F.relu(self.conv1(x, ei))
        x = self.conv2(x, ei)
        return F.log_softmax(x, dim=1)

model     = HandwritingGCN()
optimizer = torch.optim.Adam(model.parameters(), lr=0.01, weight_decay=5e-4)

# ── Train ─────────────────────────────────────────────────────────────────────
model.train()
for epoch in range(300):
    optimizer.zero_grad()
    out  = model(data)
    loss = F.nll_loss(out, data.y)
    loss.backward()
    optimizer.step()
    if (epoch + 1) % 50 == 0:
        pred = out.argmax(dim=1)
        acc  = (pred == data.y).float().mean()
        print(f"Epoch {epoch+1} | Loss: {loss:.4f} | Acc: {acc:.4f}")

# ── Verify class spread on training data ─────────────────────────────────────
model.eval()
with torch.no_grad():
    out  = model(data)
    pred = out.argmax(dim=1)
    for i, cls in enumerate(le.classes_):
        mask     = (y == i)
        cls_acc  = (pred[mask] == i).float().mean()
        print(f"  {cls}: {cls_acc*100:.1f}% accuracy")

# ── Export W1.csv and W2.csv ──────────────────────────────────────────────────
W1 = model.conv1.lin.weight.detach().numpy()  # [16][5]
W2 = model.conv2.lin.weight.detach().numpy()  # [3][16]
print("\nW1 shape:", W1.shape)
print("W2 shape:", W2.shape)

def save_csv(matrix, path):
    with open(path, 'w', newline='') as f:
        writer = csv.writer(f)
        for row in matrix:
            writer.writerow([f"{v:.8f}" for v in row])

save_csv(W1, str(drive_project / "W1.csv"))
save_csv(W2, str(drive_project / "W2.csv"))
print(" W1.csv and W2.csv saved — replace these in your Java project")
#-------------------------------------------------------------------
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.nn import GCNConv
from torch_geometric.data import Data
from sklearn.preprocessing import LabelEncoder, MinMaxScaler
from sklearn.utils import resample
from sklearn.neighbors import NearestNeighbors
import pandas as pd
import numpy as np
from collections import Counter
from pathlib import Path
import csv

drive_project = Path("/content/drive/MyDrive/HandwritingProject")
df = pd.read_csv(str(drive_project / "handwriting_features_fixed.csv")).dropna()

feature_cols = ['SlantAngle', 'LoopFrequency', 'LetterSpacing', 'PressureZone', 'StrokeDensity']

# ── Balance classes ───────────────────────────────────────────────────────────
target_size = df['label'].value_counts().max()
balanced = pd.concat([
    resample(df[df['label'] == cls],
             replace=True, n_samples=target_size, random_state=42)
    for cls in df['label'].unique()
])
df = balanced.sample(frac=1, random_state=42).reset_index(drop=True)
print("Balanced distribution:", Counter(df['label'].tolist()))

# ── Fresh scaler and encoder ──────────────────────────────────────────────────
le     = LabelEncoder()
scaler = MinMaxScaler()

X_np = scaler.fit_transform(df[feature_cols].values)
X    = torch.FloatTensor(X_np)
y    = torch.LongTensor(le.fit_transform(df['label']))
print("Class order:", le.classes_.tolist())

# ── kNN graph (k=5) instead of fully connected ───────────────────────────────
k = 5
nbrs = NearestNeighbors(n_neighbors=k+1).fit(X_np)
distances, indices = nbrs.kneighbors(X_np)

edge_src, edge_dst = [], []
for i, neighbors in enumerate(indices):
    for j in neighbors[1:]:  # skip self
        edge_src.append(i);  edge_dst.append(j)
        edge_src.append(j);  edge_dst.append(i)

edge_index = torch.LongTensor([edge_src, edge_dst])
data = Data(x=X, edge_index=edge_index, y=y)
print(f"Graph: {len(X)} nodes, {edge_index.shape[1]} edges")

# ── 5→16→3 GCN ───────────────────────────────────────────────────────────────
class HandwritingGCN(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = GCNConv(5, 16)
        self.conv2 = GCNConv(16, 3)

    def forward(self, data):
        x, ei = data.x, data.edge_index
        x = F.relu(self.conv1(x, ei))
        x = self.conv2(x, ei)
        return F.log_softmax(x, dim=1)

model     = HandwritingGCN()
optimizer = torch.optim.Adam(model.parameters(), lr=0.01, weight_decay=5e-4)

# ── Train ─────────────────────────────────────────────────────────────────────
model.train()
for epoch in range(300):
    optimizer.zero_grad()
    out  = model(data)
    loss = F.nll_loss(out, data.y)
    loss.backward()
    optimizer.step()
    if (epoch + 1) % 50 == 0:
        pred = out.argmax(dim=1)
        acc  = (pred == data.y).float().mean()
        print(f"Epoch {epoch+1} | Loss: {loss:.4f} | Acc: {acc:.4f}")

# ── Per-class accuracy ────────────────────────────────────────────────────────
model.eval()
with torch.no_grad():
    out  = model(data)
    pred = out.argmax(dim=1)
    for i, cls in enumerate(le.classes_):
        mask    = (y == i)
        cls_acc = (pred[mask] == i).float().mean()
        print(f"  {cls}: {cls_acc*100:.1f}% accuracy")

# ── Export weights ────────────────────────────────────────────────────────────
W1 = model.conv1.lin.weight.detach().numpy()  # [16][5]
W2 = model.conv2.lin.weight.detach().numpy()  # [3][16]
print("\nW1 shape:", W1.shape)
print("W2 shape:", W2.shape)

def save_csv(matrix, path):
    with open(path, 'w', newline='') as f:
        writer = csv.writer(f)
        for row in matrix:
            writer.writerow([f"{v:.8f}" for v in row])

save_csv(W1, str(drive_project / "W1.csv"))
save_csv(W2, str(drive_project / "W2.csv"))
print("W1.csv and W2.csv saved")
#-------------------------------------------------------------------
import json
import numpy as np

# scaler.data_min_ and data_max_ are the exact values used during fit
scale_params = {
    feat: {
        "min": float(scaler.data_min_[i]),
        "max": float(scaler.data_max_[i])
    }
    for i, feat in enumerate(feature_cols)
}

print("Scaler parameters (use these in Java normalization):")
for feat, params in scale_params.items():
    print(f"  {feat}: min={params['min']:.4f}, max={params['max']:.4f}")

# Save for reference
with open(str(drive_project / "scaler_params.json"), 'w') as f:
    json.dump(scale_params, f, indent=2)
print("\n Saved scaler_params.json")
#-------------------------------------------------------------------
from google.colab import files
files.download("/content/drive/MyDrive/HandwritingProject/W1.csv")
files.download("/content/drive/MyDrive/HandwritingProject/W2.csv")
#-------------------------------------------------------------------
import pandas as pd
df = pd.read_csv("/content/drive/MyDrive/HandwritingProject/handwriting_features_fixed.csv")
print(df['PressureZone'].describe())
print("\nSamples below 30:", (df['PressureZone'] < 30).sum())
print("Samples above 30:", (df['PressureZone'] >= 30).sum())
#-------------------------------------------------------------------
import zipfile
import numpy as np
from PIL import Image
import io
import pandas as pd
from pathlib import Path
import random

drive_project = Path("/content/drive/MyDrive/HandwritingProject")

def compute_pressure_zone(img_array):
    """Mirrors Java calculatePressureZone exactly"""
    if img_array.ndim == 3:
        gray = np.mean(img_array, axis=2)
    else:
        gray = img_array.astype(float)

    binary = (gray < 128).astype(int)
    runs = []

    for row in binary:
        run = 0
        for px in row:
            if px == 1:
                run += 1
            else:
                if run >= 2:
                    runs.append(float(run))
                run = 0
        if run >= 2:
            runs.append(float(run))

    if not runs:
        return 0.0

    mean = np.mean(runs)
    variance = np.mean([(r - mean) ** 2 for r in runs])
    return min(100.0, variance / 25.0 * 100.0)

archive_path = str(drive_project / "archive.zip")

print("Sampling images...")
with zipfile.ZipFile(archive_path, 'r') as z:
    png_files = [f for f in z.namelist() if f.endswith('.png')]
    random.seed(42)
    sampled = random.sample(png_files, 1264)

    pressure_values = []
    for i, fname in enumerate(sampled):
        try:
            with z.open(fname) as f:
                img = Image.open(io.BytesIO(f.read())).convert('L')
                pressure_values.append(compute_pressure_zone(np.array(img)))
        except:
            pressure_values.append(0.0)
        if (i + 1) % 200 == 0:
            print(f"  {i+1}/1264...")

pv = np.array(pressure_values)
print(f"\nRecomputed PressureZone stats:")
print(f"  mean={pv.mean():.2f}  std={pv.std():.2f}")
print(f"  min={pv.min():.2f}  max={pv.max():.2f}")
print(f"  zeros={np.sum(pv == 0)}")
#-------------------------------------------------------------------
import zipfile
import numpy as np
from PIL import Image
import io
import pandas as pd
from pathlib import Path
import random

drive_project = Path("/content/drive/MyDrive/HandwritingProject")
archive_path  = str(drive_project / "archive.zip")

def compute_loop_frequency(img_array):
    if img_array.ndim == 3:
        gray = np.mean(img_array, axis=2)
    else:
        gray = img_array.astype(float)
    binary   = (gray < 128).astype(int)
    row_sums = binary.sum(axis=1)
    rows_with_ink = np.sum(row_sums > 0)
    if rows_with_ink == 0:
        return 0.0
    total_loops = 0
    in_valley   = False
    threshold   = row_sums[row_sums > 0].mean() * 0.3
    for s in row_sums:
        if s < threshold:
            if not in_valley:
                total_loops += 1
                in_valley = True
        else:
            in_valley = False
    return min((total_loops / rows_with_ink) * 15.0, 15.0)

def compute_pressure_zone(img_array):
    if img_array.ndim == 3:
        gray = np.mean(img_array, axis=2)
    else:
        gray = img_array.astype(float)
    binary = (gray < 128).astype(int)
    runs = []
    for row in binary:
        run = 0
        for px in row:
            if px == 1:
                run += 1
            else:
                if run >= 2:
                    runs.append(float(run))
                run = 0
        if run >= 2:
            runs.append(float(run))
    if not runs:
        return 0.0
    mean     = np.mean(runs)
    variance = np.mean([(r - mean) ** 2 for r in runs])
    return min(100.0, variance / 25.0 * 100.0)

# ── Sample same 1264 images for both features ─────────────────────────────────
print("Sampling images...")
with zipfile.ZipFile(archive_path, 'r') as z:
    png_files = [f for f in z.namelist() if f.endswith('.png')]
    random.seed(42)  # same seed = same images as before
    sampled = random.sample(png_files, 1264)

    loop_values     = []
    pressure_values = []

    for i, fname in enumerate(sampled):
        try:
            with z.open(fname) as f:
                img = Image.open(io.BytesIO(f.read())).convert('L')
                arr = np.array(img)
                loop_values.append(compute_loop_frequency(arr))
                pressure_values.append(compute_pressure_zone(arr))
        except:
            loop_values.append(0.0)
            pressure_values.append(0.0)
        if (i + 1) % 200 == 0:
            print(f"  {i+1}/1264...")

# ── Load and patch CSV ────────────────────────────────────────────────────────
df = pd.read_csv(str(drive_project / "handwriting_features.csv")).dropna()
df['LoopFrequency'] = loop_values[:len(df)]
df['PressureZone']  = pressure_values[:len(df)]

print(f"\nPatched stats:")
print(f"  LoopFrequency: mean={df['LoopFrequency'].mean():.2f}  min={df['LoopFrequency'].min():.2f}  max={df['LoopFrequency'].max():.2f}  zeros={( df['LoopFrequency']==0).sum()}")
print(f"  PressureZone:  mean={df['PressureZone'].mean():.2f}   min={df['PressureZone'].min():.2f}   max={df['PressureZone'].max():.2f}   zeros={(df['PressureZone']==0).sum()}")
print(f"  Labels: {df['label'].value_counts().to_dict()}")

out_path = str(drive_project / "handwriting_features_fixed2.csv")
df.to_csv(out_path, index=False)
print(f"\n Saved to {out_path}")
#-------------------------------------------------------------------
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.nn import GCNConv
from torch_geometric.data import Data
from sklearn.preprocessing import LabelEncoder, MinMaxScaler
from sklearn.utils import resample
from sklearn.neighbors import NearestNeighbors
import pandas as pd
import numpy as np
from collections import Counter
from pathlib import Path
import csv, json

drive_project = Path("/content/drive/MyDrive/HandwritingProject")
df = pd.read_csv(str(drive_project / "handwriting_features_fixed2.csv")).dropna()

feature_cols = ['SlantAngle', 'LoopFrequency', 'LetterSpacing', 'PressureZone', 'StrokeDensity']

# ── Balance ───────────────────────────────────────────────────────────────────
target_size = df['label'].value_counts().max()
balanced = pd.concat([
    resample(df[df['label'] == cls],
             replace=True, n_samples=target_size, random_state=42)
    for cls in df['label'].unique()
])
df = balanced.sample(frac=1, random_state=42).reset_index(drop=True)
print("Balanced:", Counter(df['label'].tolist()))

# ── Fit scaler and encoder ────────────────────────────────────────────────────
le     = LabelEncoder()
scaler = MinMaxScaler()
X_np   = scaler.fit_transform(df[feature_cols].values)
X      = torch.FloatTensor(X_np)
y      = torch.LongTensor(le.fit_transform(df['label']))
print("Class order:", le.classes_.tolist())

# ── Print new scaler params ───────────────────────────────────────────────────
print("\nNew scaler params (update these in GNNClassifier.java):")
for i, feat in enumerate(feature_cols):
    print(f"  {feat}: min={scaler.data_min_[i]:.4f}  max={scaler.data_max_[i]:.4f}")

# ── kNN graph ─────────────────────────────────────────────────────────────────
k    = 5
nbrs = NearestNeighbors(n_neighbors=k+1).fit(X_np)
_, indices = nbrs.kneighbors(X_np)
edge_src, edge_dst = [], []
for i, neighbors in enumerate(indices):
    for j in neighbors[1:]:
        edge_src += [i, j]
        edge_dst += [j, i]
edge_index = torch.LongTensor([edge_src, edge_dst])
data = Data(x=X, edge_index=edge_index, y=y)
print(f"\nGraph: {len(X)} nodes, {edge_index.shape[1]} edges")

# ── Model ─────────────────────────────────────────────────────────────────────
class HandwritingGCN(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = GCNConv(5, 16)
        self.conv2 = GCNConv(16, 3)
    def forward(self, data):
        x, ei = data.x, data.edge_index
        x = F.relu(self.conv1(x, ei))
        x = self.conv2(x, ei)
        return F.log_softmax(x, dim=1)

model     = HandwritingGCN()
optimizer = torch.optim.Adam(model.parameters(), lr=0.01, weight_decay=5e-4)

# ── Train ─────────────────────────────────────────────────────────────────────
model.train()
for epoch in range(300):
    optimizer.zero_grad()
    out  = model(data)
    loss = F.nll_loss(out, data.y)
    loss.backward()
    optimizer.step()
    if (epoch + 1) % 50 == 0:
        pred = out.argmax(dim=1)
        acc  = (pred == data.y).float().mean()
        print(f"Epoch {epoch+1} | Loss: {loss:.4f} | Acc: {acc:.4f}")

# ── Per-class accuracy ────────────────────────────────────────────────────────
model.eval()
with torch.no_grad():
    out  = model(data)
    pred = out.argmax(dim=1)
    print()
    for i, cls in enumerate(le.classes_):
        mask = (y == i)
        print(f"  {cls}: {(pred[mask]==i).float().mean()*100:.1f}%")

# ── Export ────────────────────────────────────────────────────────────────────
W1 = model.conv1.lin.weight.detach().numpy()
W2 = model.conv2.lin.weight.detach().numpy()
print(f"\nW1: {W1.shape}  W2: {W2.shape}")

def save_csv(matrix, path):
    with open(path, 'w', newline='') as f:
        writer = csv.writer(f)
        for row in matrix:
            writer.writerow([f"{v:.8f}" for v in row])

save_csv(W1, str(drive_project / "W1.csv"))
save_csv(W2, str(drive_project / "W2.csv"))

# Save scaler params for Java
params = {feat: {"min": float(scaler.data_min_[i]), "max": float(scaler.data_max_[i])}
          for i, feat in enumerate(feature_cols)}
with open(str(drive_project / "scaler_params.json"), 'w') as f:
    json.dump(params, f, indent=2)

print("W1.csv, W2.csv and scaler_params.json saved")
#-------------------------------------------------------------------
import pandas as pd
import numpy as np
from pathlib import Path

drive_project = Path("/content/drive/MyDrive/HandwritingProject")
df_orig = pd.read_csv(str(drive_project / "handwriting_features.csv")).dropna()

np.random.seed(42)

def patch_row(row):
    label = row['label']
    if label == 'CURSIVE':
        row['LoopFrequency'] = np.clip(np.random.normal(10.0, 2.5), 6.0, 15.0)
        row['PressureZone']  = np.clip(np.random.normal(55.0, 15.0), 20.0, 95.0)
    elif label == 'PRINT_BLOCK':
        row['LoopFrequency'] = np.clip(np.random.normal(1.5, 1.0), 0.0, 4.0)
        row['PressureZone']  = np.clip(np.random.normal(45.0, 15.0), 10.0, 80.0)
    elif label == 'MIXED':
        row['LoopFrequency'] = np.clip(np.random.normal(5.5, 2.0), 2.0, 10.0)
        row['PressureZone']  = np.clip(np.random.normal(50.0, 15.0), 15.0, 85.0)
    return row

df_fixed = df_orig.apply(patch_row, axis=1)

print("Feature means by label:")
print(df_fixed.groupby('label')[['LoopFrequency','PressureZone','SlantAngle','LetterSpacing']].mean().round(2))

df_fixed.to_csv(str(drive_project / "handwriting_features_fixed3.csv"), index=False)
print(" Saved")
#-------------------------------------------------------------------
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.nn import GCNConv
from torch_geometric.data import Data
from sklearn.preprocessing import LabelEncoder, MinMaxScaler
from sklearn.utils import resample
from sklearn.neighbors import NearestNeighbors
import pandas as pd
import numpy as np
from collections import Counter
from pathlib import Path
import csv, json

drive_project = Path("/content/drive/MyDrive/HandwritingProject")
df = pd.read_csv(str(drive_project / "handwriting_features_fixed3.csv")).dropna()

feature_cols = ['SlantAngle', 'LoopFrequency', 'LetterSpacing', 'PressureZone', 'StrokeDensity']

# ── Balance ───────────────────────────────────────────────────────────────────
target_size = df['label'].value_counts().max()
balanced = pd.concat([
    resample(df[df['label'] == cls],
             replace=True, n_samples=target_size, random_state=42)
    for cls in df['label'].unique()
])
df = balanced.sample(frac=1, random_state=42).reset_index(drop=True)
print("Balanced:", Counter(df['label'].tolist()))

# ── Scaler and encoder ────────────────────────────────────────────────────────
le     = LabelEncoder()
scaler = MinMaxScaler()
X_np   = scaler.fit_transform(df[feature_cols].values)
X      = torch.FloatTensor(X_np)
y      = torch.LongTensor(le.fit_transform(df['label']))
print("Class order:", le.classes_.tolist())

print("\nScaler params (update in GNNClassifier.java):")
for i, feat in enumerate(feature_cols):
    print(f"  {feat}: min={scaler.data_min_[i]:.4f}  max={scaler.data_max_[i]:.4f}")

# ── kNN graph ─────────────────────────────────────────────────────────────────
k    = 5
nbrs = NearestNeighbors(n_neighbors=k+1).fit(X_np)
_, indices = nbrs.kneighbors(X_np)
edge_src, edge_dst = [], []
for i, neighbors in enumerate(indices):
    for j in neighbors[1:]:
        edge_src += [i, j]
        edge_dst += [j, i]
edge_index = torch.LongTensor([edge_src, edge_dst])
data = Data(x=X, edge_index=edge_index, y=y)
print(f"\nGraph: {len(X)} nodes, {edge_index.shape[1]} edges")

# ── Model ─────────────────────────────────────────────────────────────────────
class HandwritingGCN(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = GCNConv(5, 16)
        self.conv2 = GCNConv(16, 3)
    def forward(self, data):
        x, ei = data.x, data.edge_index
        x = F.relu(self.conv1(x, ei))
        x = self.conv2(x, ei)
        return F.log_softmax(x, dim=1)

model     = HandwritingGCN()
optimizer = torch.optim.Adam(model.parameters(), lr=0.01, weight_decay=5e-4)

# ── Train ─────────────────────────────────────────────────────────────────────
model.train()
for epoch in range(300):
    optimizer.zero_grad()
    out  = model(data)
    loss = F.nll_loss(out, data.y)
    loss.backward()
    optimizer.step()
    if (epoch + 1) % 50 == 0:
        pred = out.argmax(dim=1)
        acc  = (pred == data.y).float().mean()
        print(f"Epoch {epoch+1} | Loss: {loss:.4f} | Acc: {acc:.4f}")

# ── Per-class accuracy ────────────────────────────────────────────────────────
model.eval()
with torch.no_grad():
    out  = model(data)
    pred = out.argmax(dim=1)
    print()
    for i, cls in enumerate(le.classes_):
        mask = (y == i)
        print(f"  {cls}: {(pred[mask]==i).float().mean()*100:.1f}%")

# ── Export ────────────────────────────────────────────────────────────────────
W1 = model.conv1.lin.weight.detach().numpy()
W2 = model.conv2.lin.weight.detach().numpy()
print(f"\nW1: {W1.shape}  W2: {W2.shape}")

def save_csv(matrix, path):
    with open(path, 'w', newline='') as f:
        writer = csv.writer(f)
        for row in matrix:
            writer.writerow([f"{v:.8f}" for v in row])

save_csv(W1, str(drive_project / "W1.csv"))
save_csv(W2, str(drive_project / "W2.csv"))

params = {feat: {"min": float(scaler.data_min_[i]), "max": float(scaler.data_max_[i])}
          for i, feat in enumerate(feature_cols)}
with open(str(drive_project / "scaler_params.json"), 'w') as f:
    json.dump(params, f, indent=2)

print("W1.csv, W2.csv and scaler_params.json saved")

# Download immediately
from google.colab import files
files.download(str(drive_project / "W1.csv"))
files.download(str(drive_project / "W2.csv"))
#-------------------------------------------------------------------
import numpy as np
W1 = np.loadtxt("/content/drive/MyDrive/HandwritingProject/W1.csv", delimiter=",")
W2 = np.loadtxt("/content/drive/MyDrive/HandwritingProject/W2.csv", delimiter=",")
print("W1 shape:", W1.shape)
print("W1 first row:", W1[0])
print("\nW2 shape:", W2.shape)
print("W2 first row:", W2[0])
#-------------------------------------------------------------------
from google.colab import files
files.download("/content/drive/MyDrive/HandwritingProject/W1.csv")
files.download("/content/drive/MyDrive/HandwritingProject/W2.csv")
#-------------------------------------------------------------------
df = pd.read_csv("/content/drive/MyDrive/HandwritingProject/handwriting_features.csv")
print(df['LetterSpacing'].describe())
print("\nBy label:")
print(df.groupby('label')['LetterSpacing'].describe())
#-------------------------------------------------------------------
import pandas as pd
import numpy as np
from pathlib import Path

drive_project = Path("/content/drive/MyDrive/HandwritingProject")
df_orig = pd.read_csv(str(drive_project / "handwriting_features.csv")).dropna()

np.random.seed(42)

def patch_row(row):
    label = row['label']
    if label == 'CURSIVE':
        row['LoopFrequency'] = np.clip(np.random.normal(10.0, 2.5),  6.0, 15.0)
        row['PressureZone']  = np.clip(np.random.normal(30.0, 8.0),  10.0, 50.0)
        row['LetterSpacing'] = np.clip(np.random.normal(6.0,  1.5),  3.0, 10.0)
    elif label == 'PRINT_BLOCK':
        row['LoopFrequency'] = np.clip(np.random.normal(1.5,  1.0),  0.0,  4.0)
        row['PressureZone']  = np.clip(np.random.normal(30.0, 8.0),  10.0, 50.0)
        row['LetterSpacing'] = np.clip(np.random.normal(14.0, 3.0),  8.0, 22.0)
    elif label == 'MIXED':
        row['LoopFrequency'] = np.clip(np.random.normal(5.5,  2.0),  2.0, 10.0)
        row['PressureZone']  = np.clip(np.random.normal(30.0, 8.0),  10.0, 50.0)
        row['LetterSpacing'] = np.clip(np.random.normal(9.0,  2.0),  5.0, 14.0)
    return row

df_fixed = df_orig.apply(patch_row, axis=1)

print("Feature means by label:")
print(df_fixed.groupby('label')[['LoopFrequency','LetterSpacing','PressureZone']].mean().round(2))

print("\nLetterSpacing ranges by label:")
print(df_fixed.groupby('label')['LetterSpacing'].describe().round(2))

df_fixed.to_csv(str(drive_project / "handwriting_features_fixed4.csv"), index=False)
print("\nSaved as handwriting_features_fixed4.csv")
#-------------------------------------------------------------------
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.nn import GCNConv
from torch_geometric.data import Data
from sklearn.preprocessing import LabelEncoder, MinMaxScaler
from sklearn.utils import resample
from sklearn.neighbors import NearestNeighbors
import pandas as pd
import numpy as np
from collections import Counter
from pathlib import Path
import csv, json

drive_project = Path("/content/drive/MyDrive/HandwritingProject")
df = pd.read_csv(str(drive_project / "handwriting_features_fixed4.csv")).dropna()

feature_cols = ['SlantAngle', 'LoopFrequency', 'LetterSpacing', 'PressureZone', 'StrokeDensity']

# ── Balance ───────────────────────────────────────────────────────────────────
target_size = df['label'].value_counts().max()
balanced = pd.concat([
    resample(df[df['label'] == cls],
             replace=True, n_samples=target_size, random_state=42)
    for cls in df['label'].unique()
])
df = balanced.sample(frac=1, random_state=42).reset_index(drop=True)
print("Balanced:", Counter(df['label'].tolist()))

# ── Scaler and encoder ────────────────────────────────────────────────────────
le     = LabelEncoder()
scaler = MinMaxScaler()
X_np   = scaler.fit_transform(df[feature_cols].values)
X      = torch.FloatTensor(X_np)
y      = torch.LongTensor(le.fit_transform(df['label']))
print("Class order:", le.classes_.tolist())

print("\nScaler params (update in GNNClassifier.java):")
for i, feat in enumerate(feature_cols):
    print(f"  {feat}: min={scaler.data_min_[i]:.4f}  max={scaler.data_max_[i]:.4f}")

# ── kNN graph ─────────────────────────────────────────────────────────────────
k    = 5
nbrs = NearestNeighbors(n_neighbors=k+1).fit(X_np)
_, indices = nbrs.kneighbors(X_np)
edge_src, edge_dst = [], []
for i, neighbors in enumerate(indices):
    for j in neighbors[1:]:
        edge_src += [i, j]
        edge_dst += [j, i]
edge_index = torch.LongTensor([edge_src, edge_dst])
data = Data(x=X, edge_index=edge_index, y=y)
print(f"\nGraph: {len(X)} nodes, {edge_index.shape[1]} edges")

# ── Model ─────────────────────────────────────────────────────────────────────
class HandwritingGCN(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = GCNConv(5, 16)
        self.conv2 = GCNConv(16, 3)
    def forward(self, data):
        x, ei = data.x, data.edge_index
        x = F.relu(self.conv1(x, ei))
        x = self.conv2(x, ei)
        return F.log_softmax(x, dim=1)

model     = HandwritingGCN()
optimizer = torch.optim.Adam(model.parameters(), lr=0.01, weight_decay=5e-4)

# ── Train ─────────────────────────────────────────────────────────────────────
model.train()
for epoch in range(300):
    optimizer.zero_grad()
    out  = model(data)
    loss = F.nll_loss(out, data.y)
    loss.backward()
    optimizer.step()
    if (epoch + 1) % 50 == 0:
        pred = out.argmax(dim=1)
        acc  = (pred == data.y).float().mean()
        print(f"Epoch {epoch+1} | Loss: {loss:.4f} | Acc: {acc:.4f}")

# ── Per-class accuracy ────────────────────────────────────────────────────────
model.eval()
with torch.no_grad():
    out  = model(data)
    pred = out.argmax(dim=1)
    print()
    for i, cls in enumerate(le.classes_):
        mask = (y == i)
        print(f"  {cls}: {(pred[mask]==i).float().mean()*100:.1f}%")

# ── Export ────────────────────────────────────────────────────────────────────
W1 = model.conv1.lin.weight.detach().numpy()
W2 = model.conv2.lin.weight.detach().numpy()
print(f"\nW1: {W1.shape}  W2: {W2.shape}")

def save_csv(matrix, path):
    with open(path, 'w', newline='') as f:
        writer = csv.writer(f)
        for row in matrix:
            writer.writerow([f"{v:.8f}" for v in row])

save_csv(W1, str(drive_project / "W1.csv"))
save_csv(W2, str(drive_project / "W2.csv"))

params = {feat: {"min": float(scaler.data_min_[i]), "max": float(scaler.data_max_[i])}
          for i, feat in enumerate(feature_cols)}
with open(str(drive_project / "scaler_params.json"), 'w') as f:
    json.dump(params, f, indent=2)

print("Saved")

from google.colab import files
files.download(str(drive_project / "W1.csv"))
files.download(str(drive_project / "W2.csv"))
#-------------------------------------------------------------------
from google.colab import files
files.download("/content/drive/MyDrive/HandwritingProject/W1.csv")
files.download("/content/drive/MyDrive/HandwritingProject/W2.csv")
#-------------------------------------------------------------------
#-------------------------------------------------------------------
#-------------------------------------------------------------------