# NETWORK COMPLIANCE



## Description

This package supports capability to detect non-compliant devices on the network. It also has support to make those devices compliant by using golden(device) template(s) defined in NSO. Internally it is using built-in compliance feature in NSO. It adds addtional features like creating device groups dynamically as well as fixing configuration of non-compliant devices to make those device compliant.



## Supported Device Types
Following device types have been validated & supported by the compliance package.
Technically this package can be used with any NED, although it has been tested with XR NED.

		Device Type		Model	          Network Element Driver 	NED versions used     Device Software Version
		-----------------------------------------------------------------------------------------------------------------------
		ASR-9001 Chassis     Series (P4040)       Cisco-IOSXR                   6.4.3                  6.2.3

## Key features:
* Identify non-compliant devices
* Can use one or multiple device template for configuration comparison
* HTML report with details of compliant and non-compliant devices
* This report also contains details of missing command(s) on device(s) in case of non-compliance

## Overview

The compliance report execution consists of the following steps:

1.	Select device(s) for which you want to run compliance report.
2.	Select template(s), which you want use for compliance report.
3.	Set flag named make-compliant to true if you want to make device(s) compliant before running report.
4.	Run the report

Report will be generated and URL of report is returned. This URL can be used in browser to view the report.



## Author
**imrbaig**


## Version
1.0

## Release Date
09 - November - 2018

## Installation
* Download package from Github
* Place the downloaded package in packages directory located under your nso run directory
* Package has been tested with NCS-4.6 and java 1.8
* Make and Reload the package

## Requirements and How to Build

The package contains YANG and Python code, so you will need NSO and
Python. There are no other dependencies.

To build:

    cd src && make


## Dependencies :
* This package uses device templates. We have created few templates and stored in XML file. Please load merge file named "templates.xml" under dependencies folder into NSO before using this package.

For Example, If your templates.xml file is under /tmp/dependencies, NSO config mode command is:

    load merge /tmp/dependencies/templates.xml
		

## How to Use

Once package and dependencies has been loaded into NSO,  we are ready to use the package. Example(s) below can be used to consume the package from NSO CLI. Same package can be used from NSO GUI as well. URL returned in response to running example(s), can be used in browser to view the report.

### sample request / response
* For read only report to view non-compliant devices
##### request:
		admin@ncs% compliance-reports generate-compliance-report devices { device-name iosxr_0 } templates { template-name xr-template } make-complaint false
##### response:
        jobname		compliance-job
        url		http://localhost:8080/compliance-reports/report_87_admin_1_2018-11-13T16:56:34:0.html

* To Make device(s) compliant and run report to confirm compliance
##### request:
		admin@ncs% compliance-reports generate-compliance-report devices { device-name iosxr_0 } templates { template-name xr-template } make-complaint true
##### response:
        jobname		compliance-job
        url		http://localhost:8080/compliance-reports/report_88_admin_1_2018-1-13T16:56:34:0.html


## Demo Link :
Play recording :  Not Available at this time

## Contact Email:
For any queries & feedback, please contact the following mailer alias: as-nso-service-packs@cisco.com
