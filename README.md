# DataRefine

![](https://img.shields.io/badge/JDK-8-blueviolet)
![](https://img.shields.io/badge/Java-17-blue)

## Abstract
### Introduction

DataRefine is a Java-based spreadsheet system that allows you to load data, understand it,
clean it up, reconcile it, and augment it with data coming from
the web all from a web browser.

### Features 
1.  Outlier detection facet using nearest-neighbor (NN)-based interquantile range (IQR) for numeric data, e.g. time series, image metadata
2.  Semantic facet via the inference API of the pre-trained BERT model, e.g. people's name, stock, book title, streets
3.  Type recommendation results sorting for non-numeric data
4.  UI Renovation


## Visual Results
<p align="center">
  <a href="#">
    <img src="https://user-images.githubusercontent.com/84509949/203006750-56a8181b-1ccc-4e14-a007-a73486f69111.jpg" width="700" height="430"/>
  </a>
</p>

## 🔨 Getting Started 

1. Clone this github repo. 
```
git clone https://github.com/Joannechiao18/DataRefine.git
```
2. Install [JDK 8](https://jdk.java.net), [Apache Maven](https://maven.apache.org/), and [Eclipse](https://www.eclipse.org/downloads/).
3. Import the cloned project into Eclipse. (Remember to uncheck `extensions` and `packaging` on the import window).
4. `Run configuration` and set the base directory to `${workspace_loc:/openrefine}`; Goals to `exec:java`. 
5. Click `Run`, and DataRefine will run at local host `http://127.0.0.1:3333/`. 

## Acknowledgement

<details><summary> <b>Expand</b> </summary>
https://github.com/OpenRefine/OpenRefine
</details>

## Credits

This software was created by Metaweb Technologies, Inc. and originally written
and conceived by David Huynh <dfhuynh@google.com>. Metaweb Technologies, Inc.
was acquired by Google, Inc. in July 2010 and the product was renamed Google Refine.
In October 2012, it was renamed OpenRefine as it transitioned to a
community-supported product.

