FROM python:3.11-slim

WORKDIR /app

# Model weights are baked into the image at build time rather than downloaded
# on first request, so a cold start does not take four minutes.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

RUN python -c "from transformers import pipeline; pipeline('text-classification', model='unitary/toxic-bert', top_k=None)"

COPY app ./app

ENV PORT=8000
EXPOSE 8000

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT}"]
