import sys
from subprocess import call

if not len(sys.argv) == 2:
    print("Usage: python iconify.py <filename>")
    sys.exit(1)

filename = str(sys.argv[1])
first_part = filename.split(".")[0]

sizes = [36, 48, 72, 96, 144, 192, 512]

for s in sizes:
    output_filename = first_part + "_" + str(s) + ".png"
    call(["inkscape",
        "--export-png=" + output_filename,
        "--export-area-page",
        "--export-width=" + str(s),
        "--export-height=" + str(s),
        "--file=" + filename])
