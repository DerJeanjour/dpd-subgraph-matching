# generate
cd glema/data/data_real
python make_datasets.py --ds [DATASET_NAME]
python generate_data_v1.py --config configs/[DATASET_NAME].json

# prepare dataset (directed)
python glema/data/process_data.py --data_name KKI --real --directed

# train (directed)
python glema/training/train.py \
  --ngpu 0 \
  --directed \
  --dataset KKI \
  --batch_size 64 \
  --tactic jump \
  --embedding_dim 90

# eval (directed)
python glema/evaluation/evaluate.py \
    --ngpu 0 \
    --directed \
    --dataset KKI \
    --batch_size 64 \
    --tactic jump \
    --embedding_dim 90 \
    --ckpt glema/training/save/KKI_jump_directed/best_model.pt

