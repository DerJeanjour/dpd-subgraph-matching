cd glema/data/data_real
python make_datasets.py --ds KKI
python generate_data_v1.py --config configs/KKI.json
cd ..
python process_data.py --data_name KKI --real
python process_data.py --data_name KKI --real --directed
cd ../..