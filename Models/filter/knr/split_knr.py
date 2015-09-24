import csv
from itertools import groupby

with open("Anlage_4_Krankheitsabgrenzung_AJ2016.csv","r") as infile:
   reader = csv.reader(infile,delimiter=';')
   next(reader, None)  # skip the headers
   for key, rows in groupby(reader,
                         lambda row: row[0]):
      with open("knr%s.csv" % key, "w") as output:
         for row in rows:
            output.write(",".join(row) + "\n")
