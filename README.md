# gate_to_json

This component exports debbie annotations in XML GATE format to JSON format..

## Description  of the project

This component is only used inside the debbie pipeline because is tied up to that specific domain.

## Build and Run the Docker 

	# To build the docker, just go into the gate_to_json folder and execute
	docker build -t gate_to_json .
	#To run the docker, just set the input_folder and the output
	mkdir ${PWD}/output_annotation; docker run --rm -u $UID -v ${PWD}/input_folder:/in:ro -v ${PWD}/output_annoation:/out:rw gate_to_json gate-to-json -i /in -o /out -a MY_SET_NAME	
Parameters:
<p>
-i input folder with the documents to annotated. The documents could be plain txt or xml gate documents.
</p>
<p>
-o output folder with the documents annotated in gate format.
</p>
<p>
-a annotation set output
</p>

## Actual Version 2.0.1, 2022-11-11
## [Changelog](https://github.com/ProjectDebbie/gate_to_json/blob/master/CHANGELOG)

## Built With

* [Docker](https://www.docker.com/) - Docker Containers
* [Maven](https://maven.apache.org/) - Dependency Management

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/ProjectDebbie/gate_to_json/edit/master/nlp-standard-preprocessing/tags). 

## Authors

* **Austin McKitrick** - **Javier Corvi** 


## License

This project is licensed under the GNU GENERAL PUBLIC LICENSE Version 3 - see the [LICENSE](LICENSE) file for details..

## Funding

## Funding
<img align="left" width="75" height="50" src="eu_emblem.png"> This project has received funding from the European Union’s Horizon 2020 research and innovation programme under the Marie Sklodowska-Curie grant agreement No 751277

	
		
