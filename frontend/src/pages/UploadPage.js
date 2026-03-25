import React, { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload, FileText, CheckCircle, AlertCircle } from 'lucide-react';
import { uploadResume, saveResumeText } from '../api';

export default function UploadPage() {
  const [status, setStatus] = useState(null);
  const [message, setMessage] = useState('');
  const [textMode, setTextMode] = useState(false);
  const [resumeText, setResumeText] = useState('');

  const onDrop = useCallback(async (acceptedFiles) => {
    if (acceptedFiles.length === 0) return;
    const file = acceptedFiles[0];
    setStatus('loading');
    setMessage('');

    try {
      const res = await uploadResume(file);
      setStatus('success');
      setMessage(`Resume uploaded and chunked successfully. ID: ${res.data.id}`);
    } catch (err) {
      setStatus('error');
      setMessage(err.response?.data?.message || 'Upload failed. Please try again.');
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'application/pdf': ['.pdf'] },
    maxFiles: 1,
  });

  const handleTextSubmit = async () => {
    if (!resumeText.trim()) return;
    setStatus('loading');
    setMessage('');

    try {
      const res = await saveResumeText(resumeText);
      setStatus('success');
      setMessage(`Resume saved and chunked successfully. ID: ${res.data.id}`);
      setResumeText('');
    } catch (err) {
      setStatus('error');
      setMessage(err.response?.data?.message || 'Save failed. Please try again.');
    }
  };

  return (
    <div>
      <div className="page-header">
        <h2>Upload Resume</h2>
        <p>Upload a PDF resume or paste text to add it to the vector database</p>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 24 }}>
        <button
          className={`btn ${!textMode ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setTextMode(false)}
        >
          <Upload size={16} /> PDF Upload
        </button>
        <button
          className={`btn ${textMode ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setTextMode(true)}
        >
          <FileText size={16} /> Paste Text
        </button>
      </div>

      {!textMode ? (
        <div
          {...getRootProps()}
          className={`dropzone ${isDragActive ? 'active' : ''}`}
        >
          <input {...getInputProps()} />
          <Upload className="dropzone-icon" />
          <h3>
            {isDragActive ? 'Drop your resume here' : 'Drag & drop a PDF resume'}
          </h3>
          <p>or click to browse files</p>
        </div>
      ) : (
        <div>
          <div className="form-group">
            <label className="form-label">Resume Content</label>
            <textarea
              className="form-textarea"
              rows={10}
              placeholder="Paste resume text here..."
              value={resumeText}
              onChange={(e) => setResumeText(e.target.value)}
            />
          </div>
          <button
            className="btn btn-primary"
            onClick={handleTextSubmit}
            disabled={!resumeText.trim() || status === 'loading'}
          >
            Save Resume
          </button>
        </div>
      )}

      {status === 'loading' && (
        <div className="loading-spinner">
          <div className="spinner" />
          Processing resume and generating embeddings...
        </div>
      )}

      {status === 'success' && (
        <div className="success-msg">
          <CheckCircle size={18} />
          {message}
        </div>
      )}

      {status === 'error' && (
        <div className="error-msg">
          <AlertCircle size={18} />
          {message}
        </div>
      )}
    </div>
  );
}