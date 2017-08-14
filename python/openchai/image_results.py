import sys
execfile("/Users/steve/.ipystartup")
gitbase='/git/OCspark/'

import pandas as pd
import matplotlib.pyplot as plt
import pandasql as psql

pd.set_option('display.max_rows', 50)
pd.set_option('display.max_columns', 500)
pd.set_option('display.width', 1200)

imagesBase = '/data/scenery'

# import os, glob
# rdirs = glob.glob(imagesBase,'*.png;*.jpg')

# import Tkinter as tk
# import tkFileDialog
# root = tk.Tk()
# root.withdraw()
# file_path = tkFileDialog.askopenfilename(imagesBase)


# import wx
#
# def get_path(wildcard):
#     app = wx.App(None)
#     style = wx.FD_OPEN | wx.FD_FILE_MUST_EXIST
#     dialog = wx.FileDialog(None, 'Open', wildcard=wildcard, style=style)
#     if dialog.ShowModal() == wx.ID_OK:
#         path = dialog.GetPath()
#     else:
#         path = None
#     dialog.Destroy()
#     return path
#
# print get_path('*.txt')

from Tkinter import *
# from Tkinter.filedialog import askopenfilename
import tkFileDialog
# import pyglet

def openFile():
    # song =  filedialog.askopenfilename(filetypes = (("MP3 files", "*.mp3"),("All files","*.*")))
    # file_path = tkFileDialog.askopenfilename(imagesBase)
    file_path = tkFileDialog.askopenfilename(filetypes = (("MP3 files", "*.mp3"),("All files","*.*")))
    return file_path

f = openFile()

def openfile_dialog():
    from PyQt4 import QtGui
    app = QtGui.QApplication([dir])
    fname = QtGui.QFileDialog.getOpenFileName(None, "Select a file...", '.', filter="All files (*)")
    return str(fname)
