import glob
import os.path as osp
import random
import numpy as np
import pandas as pd
import json
from PIL import Image
from tqdm import tqdm
import matplotlib.pyplot as plt

import torch
import torch.nn as nn
import torch.optim as optim
import torch.utils.data as data
import torchvision
from torchvision import models, transforms
import onnxruntime as rt
from efficientnet_pytorch import EfficientNet
'''
torch.manual_seed(1234)
np.random.seed(1234)
random.seed(1234)
'''
class ImageTransform():

    def __init__(self, resize, mean, std):
        self.data_transform = {
            'train': transforms.Compose([
                transforms.RandomResizedCrop(
                    resize, scale=(0.5, 1.0)),  
                transforms.RandomHorizontalFlip(), 
                transforms.ToTensor(),  
                transforms.Normalize(mean, std) 
            ]),
            'valid': transforms.Compose([
                transforms.Resize(resize), 
                transforms.CenterCrop(resize), 
                transforms.ToTensor(),  
                transforms.Normalize(mean, std)  
            ]),
            'tests': transforms.Compose([
                transforms.Resize(resize), 
                transforms.CenterCrop(resize), 
                transforms.ToTensor(),  
                transforms.Normalize(mean, std)  
            ])
        }

    def __call__(self, img, phase='train'):
        return self.data_transform[phase](img)

def make_datapath_list(phase="train"):
    rootpath = "./data/project/"
    target_path = osp.join(rootpath+phase+'/**/*.jpg')

    path_list = []  

    for path in glob.glob(target_path):
        path_list.append(path)

    return path_list

class Dataset(data.Dataset):

    def __init__(self, file_list, transform=None, phase='train'):
        self.file_list = file_list 
        self.transform = transform  
        self.phase = phase 

    def __len__(self):
        return len(self.file_list)

    def __getitem__(self, index):
        img_path = self.file_list[index]
        img = Image.open(img_path)

        img_transformed = self.transform(img, self.phase) 

        '''
        "0": "sidewalk"    
        "1": "road"
        "2": "crosswalk"
        "3": "stair"
        "4": "door"
        '''

        label = img_path[21:23]

        if label == "si":
            label = 0
        elif label == "ro":
            label = 1
        elif label == "cr":
            label = 2
        elif label == "st":
            label = 3
        elif label == "do":
            label = 4

        return img_transformed, label

def test_and_visualize_model(model,dataloaders_dict, phase = 'tests', num_images=36):
    # phase = 'train', 'valid', 'tests'
    
    was_training = model.training
    model.eval()
    fig = plt.figure()
    
    running_loss, running_corrects, num_cnt = 0.0, 0, 0
    
    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    model.to(device)

    with torch.no_grad():
        for i, (inputs, labels) in enumerate(dataloaders_dict[phase]):
            inputs = inputs.to(device)
            labels = labels.to(device)

            outputs = model(inputs)
            _, preds = torch.max(outputs, 1)
            loss = criterion(outputs, labels)  # batch의 평균 loss 출력

            running_loss    += loss.item() * inputs.size(0)
            running_corrects+= torch.sum(preds == labels.data)
            num_cnt += inputs.size(0)  # batch size

    #         if i == 2: break

        test_loss = running_loss / num_cnt
        test_acc  = running_corrects.double() / num_cnt       
        print('test done : loss/acc : %.2f / %.1f' % (test_loss, test_acc*100))

    # 예시 그림 plot
    with torch.no_grad():
        for i, (inputs, labels) in enumerate(dataloaders_dict[phase]):
            inputs = inputs.to(device)
            labels = labels.to(device)

            outputs = model(inputs)
            _, preds = torch.max(outputs, 1)        

            if i == 0 :
                for j in range(1, num_images+1):
                    ax = plt.subplot(num_images//6, 6, j)
                    ax.axis('off')
                    ax.set_title('%s : %s -> %s'%(
                        'True' if class_names[str(labels[j].cpu().numpy())]==class_names[str(preds[j].cpu().numpy())] else 'False',
                        class_names[str(labels[j].cpu().numpy())], class_names[str(preds[j].cpu().numpy())]))
                    imshow(inputs.cpu().data[j])          
                break
            
    plt.tight_layout()
    plt.show()
    model.train(mode=was_training);  # 다시 train모드로


def train_model(net, dataloaders_dict, criterion, optimizer, num_epochs, num):
    history = {'val_loss': [],
               'val_acc': []}

    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    print("device：", device)

    net.to(device)

    torch.backends.cudnn.benchmark = True

    for epoch in range(num_epochs):
        print('Epoch {}/{}'.format(epoch+1, num_epochs))
        print('-------------')

        for phase in ['train', 'valid']:
            if phase == 'train':
                net.train() 
            else:
                net.eval()

            epoch_loss = 0.0 
            epoch_corrects = 0  

            if (epoch == 0) and (phase == 'train'):
                continue

            for inputs, labels in tqdm(dataloaders_dict[phase]):

                inputs = inputs.to(device)
                labels = labels.to(device)

                optimizer.zero_grad()

                with torch.set_grad_enabled(phase == 'train'):
                    outputs = net(inputs)
                    loss = criterion(outputs, labels)
                    _, preds = torch.max(outputs, 1)

                    if phase == 'train':
                        loss.backward()
                        optimizer.step()

                    epoch_loss += loss.item() * inputs.size(0) 
                    epoch_corrects += torch.sum(preds == labels.data)

            epoch_loss = epoch_loss / len(dataloaders_dict[phase].dataset)
            epoch_acc = epoch_corrects.double(
            ) / len(dataloaders_dict[phase].dataset)
            print('{} Loss: {:.4f} Acc: {:.4f}'.format(phase, epoch_loss, epoch_acc))

            if(phase == 'valid'):
                if(epoch > 1) and (max(history['val_acc']) < epoch_acc) :
                    torch.save(net.state_dict(), 'final+++' + str(num) + '.pt')
                history['val_acc'].append(epoch_acc.item())
                history['val_loss'].append(epoch_loss)

    return history

def imshow(inp, title=None):
    inp = inp.numpy().transpose((1, 2, 0))
    mean = np.array([0.485, 0.456, 0.406])
    std = np.array([0.229, 0.224, 0.225])
    inp = std * inp + mean
    inp = np.clip(inp, 0, 1)
    plt.imshow(inp)
    if title is not None:
        plt.title(title)

def visualize(epochs ,history):
    fig = plt.figure()

    ax_acc = fig.add_subplot(111)

    ax_acc.plot(range(epochs), history['val_acc'], label='Accuracy(%)', color='darkred')

    plt.text(3, 14.7, "<----------------Accuracy(%)", verticalalignment='top', horizontalalignment='right')
    plt.xlabel('epochs')
    plt.ylabel('Accuracy(%)')
    plt.axvline(history['val_acc'].index(max(history['val_acc'])), 0, 1, color='red', linestyle='solid', linewidth=2)
    plt.text(history['val_acc'].index(max(history['val_acc']))+10, 0.85, str(round(max(history['val_acc']),4)))
    plt.text(history['val_acc'].index(max(history['val_acc']))+10, 0.825, str(history['val_acc'].index(max(history['val_acc']))))
    plt.legend(loc = 'upper right', bbox_to_anchor=(0.5, 1.1))

    ax_loss = ax_acc.twinx()
    ax_loss.plot(range(epochs), history['val_loss'], label='Loss', color='darkblue')
    plt.text(3, 2.2, "<----------------Loss", verticalalignment='top', horizontalalignment='left')
    plt.ylabel('Loss')
    plt.legend(loc = 'upper right', bbox_to_anchor=(0.7, 1.1))
    ax_loss.yaxis.tick_right()

    # 그래프 표시
    plt.savefig('acc1.png')
    plt.show()

num_show_img = 5

class_names = {
    "0": "sidewalk",      
    "1": "road", 
    "2": "crosswalk",  
    "3": "stair", 
    "4": "door"  
}


train_list = make_datapath_list(phase="train")
valid_list = make_datapath_list(phase="valid")
test_list = make_datapath_list(phase="tests")

size = EfficientNet.get_image_size('efficientnet-b0')
mean = (0.485, 0.456, 0.406)
std = (0.229, 0.224, 0.225)

train_dataset = Dataset(
    file_list=train_list, transform=ImageTransform(size, mean, std), phase='train')

valid_dataset = Dataset(
    file_list=valid_list, transform=ImageTransform(size, mean, std), phase='valid')

test_dataset = Dataset(
    file_list=test_list, transform=ImageTransform(size, mean, std), phase='tests')

batch_size = 64

train_dataloader = torch.utils.data.DataLoader(
    train_dataset, batch_size=batch_size, shuffle=True)

valid_dataloader = torch.utils.data.DataLoader(
    valid_dataset, batch_size=batch_size, shuffle=False)

test_dataloader = torch.utils.data.DataLoader(
    test_dataset, batch_size=batch_size, shuffle=False)

dataloaders_dict = {"train": train_dataloader, "valid": valid_dataloader,"tests": test_dataloader}

use_pretrained = True

criterion = nn.CrossEntropyLoss()

#0.001,0.95

plt.subplot(3,1,1)
plt.ylabel("train set")
inputs, classes = next(iter(train_dataloader))
out = torchvision.utils.make_grid(inputs[:num_show_img])  
imshow(out, title=[class_names[str(int(x))] for x in classes[:num_show_img]])

plt.subplot(3,1,2)
plt.ylabel("valid set")
inputs, classes = next(iter(valid_dataloader))
out = torchvision.utils.make_grid(inputs[:num_show_img])
imshow(out, title=[class_names[str(int(x))] for x in classes[:num_show_img]])

plt.subplot(3,1,3)
plt.ylabel("test set")
inputs, classes = next(iter(test_dataloader))
out = torchvision.utils.make_grid(inputs[:num_show_img])
imshow(out, title=[class_names[str(int(x))] for x in classes[:num_show_img]])

plt.tight_layout()
plt.show()

list_lr = [0.001]
list_op = [#"sgd", 
           "adam"]

result = []
num_epochs = 300
for op in list_op:
    for lr in list_lr:
        net = EfficientNet.from_pretrained('efficientnet-b0',num_classes = 5)
        net._dropout = nn.Dropout(0.5)

        print("learing late :" + str(lr)+" momentum :"+op)
        '''
        for name, param in net.named_parameters():
            if '_fc' not in name:
                param.requires_grad = False
        '''
        if op == "adam" :
            optimizer = optim.Adam(net.parameters(),lr=lr)
        else :
            optimizer = optim.SGD(net.parameters(), lr=lr, momentum=0.95)

        history = train_model(net, dataloaders_dict, criterion, optimizer, num_epochs=num_epochs, num = str(lr) + op)
        result.append([max(history['val_acc']),lr,op])

visualize(num_epochs, history)
result.sort(key=lambda x: x[0])
for item in result:
    print('Acc: {:.4f} lr: {} momentum: {}'.format(item[0],item[1],item[2]))






    '''
net = EfficientNet.from_pretrained('efficientnet-b0',num_classes = 5)
model_state_dict = torch.load("./final+++0.0095.pt")
net.load_state_dict(model_state_dict)
test_and_visualize_model(net,dataloaders_dict, phase = 'tests')
'''

'''
net.set_swish(memory_efficient=False)
net.eval()
device = torch.device("cuda:0")
net = net.to(device)

output_onnx = 'test+++.onnx'

input_names = ["input"]

output_names = ["output"]
'''
'''
output_names = ["sidewalk",      
                "road", 
                "crosswalk",  
                "stair",
                "door"]
'''
'''

dynamic_axes = {'input_0' : {0 : 'batch_size'},
                    'output_0' : {0 : 'batch_size'}}

inputs = torch.randn(1, 3, 224, 224, requires_grad=True).to(device)

torch_out = torch.onnx._export(net, inputs, output_onnx, export_params=True, verbose=False,
                                   input_names=input_names, output_names=output_names, opset_version=10
                                   #,dynamic_axes = dynamic_axes
                                   )

def to_numpy(tensor): 
   return tensor.detach().cpu().numpy() if tensor.requires_grad else tensor.cpu().numpy() 

def test(model,onnx_model):
    x = torch.rand(1, 3, 224, 224, requires_grad=True).to(device)
    out_torch = model(x)
    
    ort_session = rt.InferenceSession(onnx_model)
    ort_inputs = {ort_session.get_inputs()[0].name: to_numpy(x)}
    ort_outs = ort_session.run(None, ort_inputs)
    
    np.testing.assert_allclose(to_numpy(out_torch), ort_outs[0], rtol=1e-03, atol=1e-05)
    print("Exported model has been tested with ONNXRuntime, and the result looks good!")

test(net,"test+++.onnx")
'''