from gradio_client import Client, handle_file
import json
import requests
import sys

def add_img(img_path):
    result = client.predict(
	    history=[],
	    file=handle_file(img_path),
	    api_name="/add_file"
    )
    file_path = result[0][0]['file']
    return [{"file":handle_file(file_path),"alt_text":''},None]

client = Client("Qwen/Qwen2-VL")

result = client.predict(
    api_name="/reset_user_input"
)

history = []

fileId = sys.argv[1]
fileName = sys.argv[2]
images = sys.argv[3]

print("args id " + fileId + " name " + fileName + " imgs " + images)

if len(fileId) == 0:
    print("no fileId provided, exiting")
    exit()

if len(fileName) == 0:
    print("no fileName provided, exiting")
    exit()

if len(images) == 0:
    print("no images provided, exiting")
    exit()

for img in images.split(","):
    print("adding " + img)
    history.append(add_img(img))

result = client.predict(
  history=history,
  text='image to text',
  api_name="/add_text"
)

#print(result)
history.append(['image to text', None])

print("predict")

result = client.predict(
  _chatbot=history,
  api_name="/predict"
)

url = "http://localhost:8080/transcript"

#fileId="";
#fileName="";
#result="";

data = {
    "fileId": fileId,
    "fileName": fileName,
    "text-content": result
}

print(data);

response = requests.post(url, json=data)

#print("Status Code", response.status_code)
#print("JSON Response ", response.json())
