"use client";

import React from "react";
import "./loading-turtle.css";

interface LoadingTurtleProps {
  size?: number;
  className?: string;
}

export const LoadingTurtle: React.FC<LoadingTurtleProps> = ({ size = 64, className = "" }) => (
  <div
    className={`loading-turtle ${className}`}
    style={{
      width: `${size}px`,
      height: `${size}px`,
    }}
    aria-label="Loading"
    role="status"
  />
);
