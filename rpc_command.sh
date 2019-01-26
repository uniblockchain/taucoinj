#!/bin/bash
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#/
#this is only used to test,.....,when you try to use it please make sure that localhost is free
#curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "tau_protocolVersion", "params": [] }'  http://127.0.0.1:8606
#curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "tau_accounts", "params": [] }'  http://127.0.0.1:8606
curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "tau_newaccount", "params": [] }'  http://127.0.0.1:8606
#curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "tau_importprikey", "params": [70375058633862235424321040800080181186779250603075941646381306644335108412868] }'  http://127.0.0.1:8606

#send raw transaction
#curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "tau_sendTransaction", "params": [{"to":"0xabcdefabcdefabcdefababcdefabcdefabcdefab", "value": 10000, "fee": 100, "privkey":"0xd25ed9f789974c6c5c7e7204bfcc5a458f2e3484d52b0eab93b5fd014edf705b"}] }'  http://127.0.0.1:8606/

#get transactions in pendingState
#curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "tau_getTransactions", "params": [] }'  http://127.0.0.1:8606

# Start forging
#curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "tau_forging", "params": [{"amount": 1}] }'  http://127.0.0.1:8606/

# get block hash list
#curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "tau_getBlockHashList", "params": [{"start": 0}] }'  http://127.0.0.1:8606/
#curl --data-binary '{"jsonrpc": "2.0", "id":"1", "method": "db_getbestblock", "params": [] }'  http://127.0.0.1:8606/
