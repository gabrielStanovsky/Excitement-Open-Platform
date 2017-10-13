""" Usage:
    snli_to_rte --in=INPUT_FILE --out=OUTPUT_FILE

Convert SNLI jsnol format to RTE's xml format.
"""
# External imports
import logging
from pprint import pprint
from pprint import pformat
from docopt import docopt
import xml.etree.cElementTree as ET
import json

# Local imports

# ---

# Mapping from SNLI to RTE gold values
SNLI_TAG_TO_RTE = {
    "contradiction": "NONENTAILMENT",
    "entailment": "ENTAILMENT"}
#    "neutral":} # TODO: IS THERE A THREE WAY OPTION?

def convert(inp_fn,
            out_fn):
    """
    Convert input jsonl SNLI to RTE XML
    """
    root = ET.Element("entailment-corpus",
                      lang="EN")
    tree = ET.ElementTree(root)

    # Parse input jsonl file
    samples = [json.loads(l)
               for l in open(inp_fn)]

    # Populate XML
    for sample_ind, sample in enumerate([sample
                                         for sample in samples
                                         if sample["gold_label"] in SNLI_TAG_TO_RTE]):
        # Filter out samples which we don't know how to convert
        cur_pair = ET.SubElement(root,
                                 "pair",
                                 id = str(sample_ind),
                                 entailment = SNLI_TAG_TO_RTE[sample["gold_label"]],
                                 task = "IE") # TODO: Does the task attribute matter?
        #TODO: Figure out what's T and what's H in SNLI
        ET.SubElement(cur_pair,
                      "t").text = sample["sentence1"]
        ET.SubElement(cur_pair,
                      "h").text = sample["sentence2"]

    # Write to file
    tree.write(out_fn)
    logging.info("XML file wrote to {}".format(out_fn))

if __name__ == "__main__":
    logging.basicConfig(level = logging.DEBUG)

    # Parse command line arguments
    args = docopt(__doc__)
    inp_fn = args["--in"]
    out_fn = args["--out"]

    # Convert and write to file
    convert(inp_fn,
            out_fn)

    logging.info("DONE")
