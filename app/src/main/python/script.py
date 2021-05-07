import requests
import json
access_token = ""
#the function to send sounds
def send(link, filepath, filename, title, description, name, id):
    #set the params as including the access token
    params = {'access_token': access_token}
    #open the file
    with open(filepath, "rb") as fp:
        #send the file
        requests.put(
            "%s/%s" % (link, filename),
            data=fp,
            params=params,
        )
    #set the metadata we want to add to the file
    metadata = {
        "metadata": {
            "title": title,
            "upload_type": "video",
            "description": description,
            "creators": [
                {"name": name}
            ]
        }
    }
    #add the metadata
    url = "https://sandbox.zenodo.org/api/deposit/depositions/%s?%s" % (id, access_token)
    headers = {"Content-Type": "application/json"}
    r = requests.put(url, params=params, data=json.dumps(metadata), headers=headers)

#the function to send waypoints
def sendway(link, title, description, name, id, lat, lon, file):
    #set the params as including the access token
    params = {'access_token': access_token}
    #create the blank file
    file1 = open(file, "w")
    file1.write("blank")
    file1.close()
    with open(file, "rb") as f:
        #send the file
        requests.put(
            "%s/%s" % (link, "blank.txt"),
            data=f,
            params=params,
        )

    url = "https://sandbox.zenodo.org/api/deposit/depositions/%s?%s" % (id, access_token)
    headers = {"Content-Type": "application/json"}
    #set the metadata we want to add to the file
    metadata = {
        "metadata": {
            "title": title,
            "upload_type": "other",
            "description": description,
            "creators": [
                {"name": name}
            ],
            "locations": [{"lat": lat, "lon": lon, "place": title}],
            "communities": [{'identifier': 'jiu-valley-sounds'}]
        }
    }
    #add the metadata
    r = requests.put(url, params=params, data=json.dumps(metadata), headers=headers)

    #uncomment this block if you want to automatically accept waypoints
    #requests.post('https://sandbox.zenodo.org/api/deposit/depositions/%s/actions/publish' % id,
    #              params={'access_token': access_token} )
