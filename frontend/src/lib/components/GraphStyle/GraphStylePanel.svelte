<script>
    const { styleConfig } = $props();
</script>

<div class="panel">
    <div class="section">
        <h3 class="section-title">Node Styling</h3>

        <div class="control-group">
            <label for="node-size" class="control-label">
                Size
                <span class="value-badge">{styleConfig.node.size}px</span>
            </label>
            <input
                id="node-size"
                type="range"
                min="20"
                max="100"
                bind:value={styleConfig.node.size}
                class="slider"
            />
        </div>

        <div class="control-group">
            <label for="node-color" class="control-label"> Node Color </label>
            <div class="color-input-wrapper">
                <input
                    id="node-color"
                    type="color"
                    bind:value={styleConfig.node.color}
                    class="color-input"
                />
                <span class="color-value">{styleConfig.node.color}</span>
            </div>
        </div>

        <div class="control-group">
            <label for="node-text" class="control-label"> Text Color </label>
            <div class="color-input-wrapper">
                <input
                    id="node-text"
                    type="color"
                    bind:value={styleConfig.node.textColor}
                    class="color-input"
                />
                <span class="color-value">{styleConfig.node.textColor}</span>
            </div>
        </div>
    </div>

    <div class="divider"></div>

    <div class="section">
        <h3 class="section-title">Edge Styling</h3>

        <div class="control-group">
            <label for="edge-width" class="control-label">
                Width
                <span class="value-badge"
                    >{styleConfig.edge.width.toFixed(1)}</span
                >
            </label>
            <input
                id="edge-width"
                type="range"
                min="0.2"
                max="10"
                step="0.2"
                bind:value={styleConfig.edge.width}
                class="slider"
            />
        </div>

        <div class="control-group">
            <label for="covered-color" class="control-label">
                Covered Color
            </label>
            <div class="color-input-wrapper">
                <input
                    id="covered-color"
                    type="color"
                    bind:value={styleConfig.edge.coveredColor}
                    class="color-input"
                />
                <span class="color-value">{styleConfig.edge.coveredColor}</span>
            </div>
        </div>

        <div class="control-group">
            <label for="uncovered-color" class="control-label">
                Uncovered Color
            </label>
            <div class="color-input-wrapper">
                <input
                    id="uncovered-color"
                    type="color"
                    bind:value={styleConfig.edge.uncoveredColor}
                    class="color-input"
                />
                <span class="color-value"
                    >{styleConfig.edge.uncoveredColor}</span
                >
            </div>
        </div>

        <div class="control-group">
            <label for="curve-style" class="control-label"> Curve Style </label>
            <select
                id="curve-style"
                bind:value={styleConfig.edge.curveStyle}
                class="select"
            >
                <option value="bezier">Bezier</option>
                <option disabled value="unbundled-bezier"
                    >Unbundled Bezier</option
                >
                <option disabled value="segments">Segments</option>
                <option disabled value="straight">Straight</option>
            </select>
        </div>

        <div class="control-group">
            <label for="curve-spacing" class="control-label">
                Curve Spacing
                <span class="value-badge"
                    >{styleConfig.edge.controlPointDistance}</span
                >
            </label>
            <input
                id="curve-spacing"
                type="range"
                min="0"
                max="150"
                bind:value={styleConfig.edge.controlPointDistance}
                class="slider"
            />
        </div>
    </div>

    <div class="divider"></div>

    <div class="section">
        <h3 class="section-title">Label Styling</h3>

        <div class="control-group">
            <label for="label-font-size" class="control-label">
                Font Size
                <span class="value-badge">{styleConfig.label.fontSize}</span>
            </label>
            <input
                id="label-font-size"
                type="range"
                min="1"
                max="4"
                step="0.5"
                bind:value={styleConfig.label.fontSize}
                class="slider"
            />
        </div>

        <div class="control-group">
            <label for="label-bg-color" class="control-label">
                Background Color
            </label>
            <div class="color-input-wrapper">
                <input
                    id="label-bg-color"
                    type="color"
                    bind:value={styleConfig.label.backgroundColor}
                    class="color-input"
                />
                <span class="color-value"
                    >{styleConfig.label.backgroundColor}</span
                >
            </div>
        </div>

        <div class="control-group">
            <label for="label-text-color" class="control-label">
                Text Color
            </label>
            <div class="color-input-wrapper">
                <input
                    id="label-text-color"
                    type="color"
                    bind:value={styleConfig.label.textColor}
                    class="color-input"
                />
                <span class="color-value">{styleConfig.label.textColor}</span>
            </div>
        </div>

        <div class="control-group">
            <label for="label-border-color" class="control-label">
                Border Color
            </label>
            <div class="color-input-wrapper">
                <input
                    id="label-border-color"
                    type="color"
                    bind:value={styleConfig.label.borderColor}
                    class="color-input"
                />
                <span class="color-value">{styleConfig.label.borderColor}</span>
            </div>
        </div>

        <div class="control-group">
            <label for="label-border-width" class="control-label">
                Border Width
                <span class="value-badge"
                    >{(styleConfig.label.borderWidth ?? 1).toFixed(1)}px</span
                >
            </label>
            <input
                id="label-border-width"
                type="range"
                min="0"
                max="4"
                step="0.2"
                bind:value={styleConfig.label.borderWidth}
                class="slider"
            />
        </div>

        <div class="control-group">
            <label for="label-opacity" class="control-label">
                Opacity
                <span class="value-badge"
                    >{(styleConfig.label.opacity * 100).toFixed(0)}%</span
                >
            </label>
            <input
                id="label-opacity"
                type="range"
                min="0"
                max="1"
                step="0.05"
                bind:value={styleConfig.label.opacity}
                class="slider"
            />
        </div>

        <div class="control-group">
            <label for="label-padding" class="control-label">
                Padding
                <span class="value-badge">{styleConfig.label.padding}px</span>
            </label>
            <input
                id="label-padding"
                type="range"
                min="0"
                max="8"
                bind:value={styleConfig.label.padding}
                class="slider"
            />
        </div>
    </div>
</div>

<style>
    .panel {
        display: flex;
        flex-direction: column;
        gap: 1rem;
    }

    .section {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
    }

    .section-title {
        font-size: 0.875rem;
        font-weight: 700;
        color: #1f2937;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        margin: 0.5rem 0 0.25rem 0;
    }

    .divider {
        height: 1px;
        background: linear-gradient(
            to right,
            transparent,
            #d1d5db,
            transparent
        );
    }

    .control-group {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
    }

    .control-label {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 0.8125rem;
        font-weight: 500;
        color: #374151;
        cursor: pointer;
    }

    .value-badge {
        background: #f3f4f6;
        padding: 0.25rem 0.625rem;
        border-radius: 0.375rem;
        font-size: 0.75rem;
        font-weight: 600;
        color: #6b7280;
        font-family: "Monaco", "Courier New", monospace;
    }

    .slider {
        width: 100%;
        height: 6px;
        border-radius: 3px;
        background: linear-gradient(90deg, #e5e7eb 0%, #d1d5db 100%);
        outline: none;
        -webkit-appearance: none;
        appearance: none;
    }

    .slider::-webkit-slider-thumb {
        -webkit-appearance: none;
        appearance: none;
        width: 16px;
        height: 16px;
        border-radius: 50%;
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
        cursor: pointer;
        box-shadow: 0 2px 4px rgba(59, 130, 246, 0.4);
        transition: transform 0.2s;
    }

    .slider::-webkit-slider-thumb:hover {
        transform: scale(1.2);
    }

    .slider::-moz-range-thumb {
        width: 16px;
        height: 16px;
        border-radius: 50%;
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
        cursor: pointer;
        border: none;
        box-shadow: 0 2px 4px rgba(59, 130, 246, 0.4);
        transition: transform 0.2s;
    }

    .slider::-moz-range-thumb:hover {
        transform: scale(1.2);
    }

    .color-input-wrapper {
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    .color-input {
        width: 48px;
        height: 36px;
        border: 2px solid #e5e7eb;
        border-radius: 0.5rem;
        cursor: pointer;
        transition: border-color 0.2s;
    }

    .color-input:hover {
        border-color: #3b82f6;
    }

    .color-value {
        font-size: 0.8125rem;
        font-family: "Monaco", "Courier New", monospace;
        color: #6b7280;
        font-weight: 500;
        flex: 1;
    }

    .select {
        padding: 0.5rem;
        border: 1px solid #e5e7eb;
        border-radius: 0.5rem;
        font-size: 0.8125rem;
        background-color: white;
        color: #1f2937;
        cursor: pointer;
        transition:
            border-color 0.2s,
            box-shadow 0.2s;
        font-family: inherit;
    }

    .select:hover {
        border-color: #3b82f6;
    }

    .select:focus {
        outline: none;
        border-color: #3b82f6;
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }
</style>
