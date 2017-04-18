import sys
from subprocess import call

if len(sys.argv) < 2 or len(sys.argv) > 3:
    print("Usage: python iconify.py <filename> [name]")
    sys.exit(1)

filename = str(sys.argv[1])
if len(sys.argv) == 3:
    output = str(sys.argv[2])
else:
    output = filename.split(".")[0]

sizes = [("l", 36), ("m", 48), ("h", 72), ("xh", 96), ("xxh", 144), ("xxxh", 192), ("Z", 512)]

for (n, s) in sizes:
    d = "mipmap-" + n + "dpi"
    call(["mkdir", "-p", d])
    output_filename = d + "/" + output + ".png"
    call(["inkscape",
        "--export-png=" + output_filename,
        "--export-area-page",
        "--export-width=" + str(s),
        "--export-height=" + str(s),
        "--file=" + filename])
